import pytest
from fastapi.testclient import TestClient

from app.diabetes import clinical_floor
from app.main import app


@pytest.fixture(scope="module")
def client():
    with TestClient(app) as c:
        yield c


def payload(**overrides):
    body = {
        "gender": "Male", "age": 46, "hypertension": 1, "heart_disease": 0, "smoking_history": "former",
        "bmi": 28.4, "hba1c_level": 6.4, "blood_glucose_level": 132,
    }
    body.update(overrides)
    return body


def test_health_reports_model_and_index(client):
    body = client.get("/health").json()
    assert body["status"] == "ok"
    assert body["policy_chunks"] > 100


def test_healthy_young_adult_is_low_risk(client):
    body = client.post("/predict/diabetes", json=payload(
        gender="Female", age=28, hypertension=0, smoking_history="never", bmi=22.0, hba1c_level=5.1,
        blood_glucose_level=88)).json()
    assert body["risk_band"] == "LOW"
    assert body["probability"] < 0.2


def test_diabetic_range_values_are_never_reported_below_high(client):
    body = client.post("/predict/diabetes", json=payload(hba1c_level=6.5, blood_glucose_level=110)).json()
    assert body["risk_band"] in ("HIGH", "VERY_HIGH")


def test_risk_rises_with_hba1c(client):
    probs = [client.post("/predict/diabetes", json=payload(hba1c_level=h)).json()["probability"]
             for h in (5.0, 5.7, 6.4, 7.0)]
    assert probs == sorted(probs)


def test_invalid_input_is_rejected(client):
    assert client.post("/predict/diabetes", json=payload(gender="Robot")).status_code == 422
    assert client.post("/predict/diabetes", json=payload(age=500)).status_code == 422


def test_pdf_ligature_artifacts_are_cleaned():
    from app.assistant import clean_pdf_text

    assert clean_pdf_text("covered only a/fter the prima/r_y period") == "covered only after the primary period"
    assert clean_pdf_text("Insured and/or nominee") == "Insured and/or nominee"


def test_clinical_floor_thresholds():
    assert clinical_floor(5.2, 90) == "LOW"
    assert clinical_floor(5.8, 90) == "MODERATE"
    assert clinical_floor(5.2, 130) == "HIGH"


def test_insurance_question_returns_cited_sources(client):
    body = client.post("/assistant/chat", json={
        "message": "What is the waiting period for pre-existing diseases in Activ One?"}).json()
    assert body["sources"]
    assert all(s["policy"] == "Activ One" for s in body["sources"])


def test_vector_search_finds_paraphrased_questions(client):
    from app.main import state

    index = state["bot"].index
    assert index.vectors is not None, "FAISS index should load"
    # The question never says "maternity"; keyword search alone misses it
    hits = index.search("Will my insurer pay if I have a baby?", k=3)
    assert any("maternity" in c.text.lower() for c, _ in hits)


def test_keyword_search_still_works_without_vectors():
    from app.assistant import PolicyIndex

    index = PolicyIndex(vectors=False)
    assert index.mode == "keyword (TF-IDF)"
    hits = index.search("room rent limit", "Care Supreme", k=2)
    assert hits and all(c.policy == "Care Supreme" for c, _ in hits)


def test_symptoms_suggest_a_specialist(client):
    body = client.post("/assistant/chat", json={"message": "I get knee pain when climbing stairs"}).json()
    assert body["suggested_specialization"] == "Orthopedics"


def test_emergency_gets_safety_message(client):
    body = client.post("/assistant/chat", json={"message": "My father has severe chest pain right now"}).json()
    assert body["mode"] == "safety"
    assert "112" in body["reply"]
