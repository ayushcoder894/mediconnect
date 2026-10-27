"""Diabetes risk prediction with the LightGBM model from diabetes-prediction-ml.

Two safeguards sit on top of the raw model:

1. Local smoothing. The training data only contains a few discrete HbA1c and
   glucose values, so the trees jump between them (HbA1c 6.3 -> 93%, 6.5 -> 9%).
   We average predictions over a small grid of nearby inputs, which gives a
   risk curve that rises steadily with the lab values.
2. A clinical floor. The reported band is never lower than what the ADA
   diagnostic thresholds imply, so a lab value in the diabetic range can never
   be reported as low risk.
"""

from __future__ import annotations

import itertools
import pickle
from dataclasses import dataclass
from pathlib import Path

import pandas as pd

MODEL_VERSION = "lightgbm-1.0+smoothing"
MODEL_DIR = Path(__file__).resolve().parent.parent / "models" / "diabetes"

FEATURES = ["gender", "age", "hypertension", "heart_disease", "smoking_history", "bmi",
            "HbA1c_level", "blood_glucose_level"]
HBA1C_OFFSETS = [-0.3, -0.2, -0.1, 0.0, 0.1, 0.2, 0.3]
GLUCOSE_OFFSETS = [-15, -10, -5, 0, 5, 10, 15]

BANDS = ["LOW", "MODERATE", "HIGH", "VERY_HIGH"]


@dataclass(frozen=True)
class Inputs:
    gender: str
    age: int
    hypertension: int
    heart_disease: int
    smoking_history: str
    bmi: float
    hba1c_level: float
    blood_glucose_level: int


@dataclass(frozen=True)
class Prediction:
    probability: float
    model_probability: float
    risk_band: str
    factors: list[str]
    recommendation: str


def band_for(probability: float) -> str:
    if probability < 0.2:
        return "LOW"
    if probability < 0.5:
        return "MODERATE"
    if probability < 0.8:
        return "HIGH"
    return "VERY_HIGH"


def clinical_floor(hba1c: float, glucose: int) -> str:
    """Minimum band implied by ADA thresholds (HbA1c %, fasting glucose mg/dL)."""
    if hba1c >= 6.5 or glucose >= 126:
        return "HIGH"
    if hba1c >= 5.7 or glucose >= 100:
        return "MODERATE"
    return "LOW"


def explain(x: Inputs) -> list[str]:
    factors = []
    if x.hba1c_level >= 6.5:
        factors.append(f"HbA1c of {x.hba1c_level}% is in the diabetic range (6.5% or higher)")
    elif x.hba1c_level >= 5.7:
        factors.append(f"HbA1c of {x.hba1c_level}% is in the prediabetic range (5.7-6.4%)")
    if x.blood_glucose_level >= 126:
        factors.append(f"Fasting glucose of {x.blood_glucose_level} mg/dL is in the diabetic range (126 or higher)")
    elif x.blood_glucose_level >= 100:
        factors.append(f"Fasting glucose of {x.blood_glucose_level} mg/dL is above normal (100-125)")
    if x.bmi >= 30:
        factors.append(f"BMI of {x.bmi:.1f} is in the obese range")
    elif x.bmi >= 25:
        factors.append(f"BMI of {x.bmi:.1f} is in the overweight range")
    if x.age >= 45:
        factors.append("Age 45 or older")
    if x.hypertension:
        factors.append("History of high blood pressure")
    if x.heart_disease:
        factors.append("History of heart disease")
    if x.smoking_history in ("current", "ever"):
        factors.append("Smoking")
    return factors or ["No major risk factors found in these values"]


RECOMMENDATIONS = {
    "LOW": "Your risk looks low. Keep up regular activity and a balanced diet, and repeat an HbA1c test every 3 years.",
    "MODERATE": "Your risk is moderate. Small changes help: 150 minutes of exercise a week, fewer sugary drinks, "
                "and a repeat HbA1c in 6-12 months.",
    "HIGH": "Your risk is high. Please see an endocrinologist to confirm with repeat tests and plan treatment early.",
    "VERY_HIGH": "Your values strongly suggest diabetes. Please book an endocrinologist soon for confirmation and treatment.",
}


class DiabetesModel:
    def __init__(self, model_dir: Path = MODEL_DIR):
        with open(model_dir / "best_diabetes_model.pkl", "rb") as f:
            self.model = pickle.load(f)
        with open(model_dir / "diabetes_scaler.pkl", "rb") as f:
            self.scaler = pickle.load(f)
        with open(model_dir / "diabetes_encoders.pkl", "rb") as f:
            self.encoders = pickle.load(f)
        self.scaled_columns = list(self.scaler.feature_names_in_)

    def predict(self, x: Inputs) -> Prediction:
        gender = self.encoders["gender"].transform([x.gender])[0]
        smoking = self.encoders["smoking_history"].transform([x.smoking_history])[0]
        rows = [
            {
                "gender": gender,
                "age": x.age,
                "hypertension": x.hypertension,
                "heart_disease": x.heart_disease,
                "smoking_history": smoking,
                "bmi": x.bmi,
                "HbA1c_level": x.hba1c_level + dh,
                "blood_glucose_level": x.blood_glucose_level + dg,
            }
            for dh, dg in itertools.product(HBA1C_OFFSETS, GLUCOSE_OFFSETS)
        ]
        frame = pd.DataFrame(rows, columns=FEATURES)
        frame[self.scaled_columns] = self.scaler.transform(frame[self.scaled_columns])
        probabilities = self.model.predict_proba(frame)[:, 1]

        center = len(rows) // 2
        smoothed = float(probabilities.mean())
        model_band = band_for(smoothed)
        floor = clinical_floor(x.hba1c_level, x.blood_glucose_level)
        band = max(model_band, floor, key=BANDS.index)
        return Prediction(
            probability=round(smoothed, 4),
            model_probability=round(float(probabilities[center]), 4),
            risk_band=band,
            factors=explain(x),
            recommendation=RECOMMENDATIONS[band],
        )
