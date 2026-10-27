-- Reference data: hospitals, blood and organ stock, lab test catalog

INSERT INTO hospitals (id, name, city, address, phone) VALUES
 (1, 'MediConnect Central Hospital', 'Karnal',    'Sector 12, Karnal, Haryana',          '+91-184-2200100'),
 (2, 'City Care Multispeciality',     'Delhi',     'Saket, New Delhi',                    '+91-11-41001000'),
 (3, 'Sunrise Heart Institute',       'Chandigarh','Sector 17, Chandigarh',               '+91-172-5051000'),
 (4, 'Lifeline General Hospital',     'Gurugram',  'DLF Phase 2, Gurugram, Haryana',      '+91-124-4300200'),
 (5, 'Apex Kidney & Transplant Centre','Delhi',    'Rajouri Garden, New Delhi',           '+91-11-45002000'),
 (6, 'Green Valley Hospital',         'Panipat',   'GT Road, Panipat, Haryana',           '+91-180-2650300');

INSERT INTO blood_stock (hospital_id, blood_group, units) VALUES
 (1,'A+',24),(1,'A-',4),(1,'B+',31),(1,'B-',3),(1,'O+',40),(1,'O-',6),(1,'AB+',12),(1,'AB-',2),
 (2,'A+',35),(2,'A-',7),(2,'B+',28),(2,'B-',5),(2,'O+',52),(2,'O-',9),(2,'AB+',15),(2,'AB-',3),
 (3,'A+',18),(3,'A-',2),(3,'B+',22),(3,'B-',1),(3,'O+',30),(3,'O-',4),(3,'AB+',8),(3,'AB-',0),
 (4,'A+',20),(4,'A-',5),(4,'B+',19),(4,'B-',2),(4,'O+',33),(4,'O-',5),(4,'AB+',9),(4,'AB-',1),
 (5,'A+',14),(5,'A-',3),(5,'B+',16),(5,'B-',2),(5,'O+',25),(5,'O-',3),(5,'AB+',6),(5,'AB-',1),
 (6,'A+',11),(6,'A-',1),(6,'B+',13),(6,'B-',0),(6,'O+',21),(6,'O-',2),(6,'AB+',5),(6,'AB-',0);

INSERT INTO organ_availability (hospital_id, organ, available) VALUES
 (1,'Kidney',2),(1,'Cornea',5),(1,'Liver',1),
 (2,'Kidney',3),(2,'Liver',2),(2,'Heart',1),(2,'Cornea',8),
 (3,'Heart',2),(3,'Lungs',1),(3,'Cornea',3),
 (4,'Kidney',1),(4,'Liver',1),(4,'Cornea',4),
 (5,'Kidney',5),(5,'Pancreas',1),(5,'Liver',2),
 (6,'Cornea',2),(6,'Kidney',1);

-- Routine pathology tests (parameters drive the result entry form and report)
INSERT INTO lab_tests (code, name, category, description, price, sample_type, turnaround_hours, parameters) VALUES
 ('HBA1C',  'HbA1c (Glycated Haemoglobin)', 'Diabetes', 'Average blood sugar over the last 2-3 months. Key marker for diabetes screening and control.', 450, 'Blood', 12, 'HbA1c|%|4.0-5.6'),
 ('FBS',    'Fasting Blood Glucose', 'Diabetes', 'Blood sugar after 8-10 hours of fasting.', 100, 'Blood', 6, 'Blood Glucose|mg/dL|70-99'),
 ('DSP',    'Diabetes Screening Panel', 'Diabetes', 'HbA1c and fasting glucose together. Feeds the MediConnect diabetes risk check.', 499, 'Blood', 12, 'HbA1c|%|4.0-5.6;Blood Glucose|mg/dL|70-99'),
 ('CBC',    'Complete Blood Count', 'Routine', 'Haemoglobin, white cells and platelets. Screens for anaemia and infection.', 350, 'Blood', 8, 'Haemoglobin|g/dL|13.0-17.0;WBC Count|10^3/uL|4.0-11.0;Platelets|10^3/uL|150-410'),
 ('LIPID',  'Lipid Profile', 'Heart', 'Cholesterol and triglycerides for heart-disease risk.', 600, 'Blood', 12, 'Total Cholesterol|mg/dL|0-200;LDL Cholesterol|mg/dL|0-100;HDL Cholesterol|mg/dL|40-60;Triglycerides|mg/dL|0-150'),
 ('THYROID','Thyroid Profile (T3, T4, TSH)', 'Hormones', 'Thyroid function screening.', 550, 'Blood', 24, 'TSH|uIU/mL|0.4-4.0;T3|ng/dL|80-200;T4|ug/dL|5.0-12.0'),
 ('LFT',    'Liver Function Test', 'Routine', 'Liver enzymes and bilirubin.', 700, 'Blood', 12, 'ALT (SGPT)|U/L|7-56;AST (SGOT)|U/L|10-40;Total Bilirubin|mg/dL|0.1-1.2'),
 ('KFT',    'Kidney Function Test', 'Routine', 'Creatinine and urea to assess kidney function.', 650, 'Blood', 12, 'Creatinine|mg/dL|0.7-1.3;Blood Urea|mg/dL|7-20'),
 ('VITD',   'Vitamin D (25-OH)', 'Vitamins', 'Vitamin D level for bone health.', 1200, 'Blood', 24, 'Vitamin D|ng/mL|30-100'),
 ('VITB12', 'Vitamin B12', 'Vitamins', 'Vitamin B12 level for nerve and blood health.', 900, 'Blood', 24, 'Vitamin B12|pg/mL|200-900'),
 ('URINE',  'Urine Routine Examination', 'Routine', 'Physical, chemical and microscopic urine analysis.', 200, 'Urine', 6, 'Urine Protein|mg/dL|0-14;Urine Glucose|mg/dL|0-15');

-- Genetic tests from the Genomics Research & Diagnostics Centre rate list
INSERT INTO lab_tests (code, name, category, description, price, partner_price, sample_type, turnaround_hours, parameters) VALUES
 ('GEN-DNAW',  'DNA Wellness Test', 'Genetics', 'Comprehensive wellness analysis. Add-ons: Kidney Health, Cardiomet, Autoimmune, Skin, Immunity, Sleep.', 13999, 11199, 'Saliva', 336, NULL),
 ('GEN-SV',    'Sample Viability', 'Genetics', 'Assessment of sample quality and viability for genetic testing.', 9999, 4999, 'Blood', 120, NULL),
 ('GEN-STR',   'STR Analysis', 'Genetics', 'Short Tandem Repeat analysis for DNA profiling, identity verification and relationship testing.', 7999, 6399, 'Buccal swab', 120, NULL),
 ('GEN-PAT',   'Paternity Test', 'Relationship DNA', 'DNA testing to establish the biological relationship between father and child.', 11999, 9599, 'Buccal swab', 120, NULL),
 ('GEN-PPAT',  'Prenatal Paternity', 'Relationship DNA', 'Non-invasive prenatal paternity testing during pregnancy.', 129999, 116999, 'Blood', 240, NULL),
 ('GEN-MAT',   'Maternity Test', 'Relationship DNA', 'DNA testing to establish the biological relationship between mother and child.', 11999, 9599, 'Buccal swab', 120, NULL),
 ('GEN-SIB',   'Siblings Test', 'Relationship DNA', 'Determines whether two individuals share one or both biological parents.', 17999, 14399, 'Buccal swab', 168, NULL),
 ('GEN-GP',    'Grandparentage Test', 'Relationship DNA', 'Establishes the biological relationship between grandparents and grandchild.', 17999, 14399, 'Buccal swab', 168, NULL),
 ('GEN-AVU',   'Avuncular Test', 'Relationship DNA', 'Biological relationship between aunt/uncle and niece/nephew.', 17999, 14399, 'Buccal swab', 168, NULL),
 ('GEN-YSTR',  'YSTR Analysis', 'Genetics', 'Y-chromosome STR analysis for paternal lineage testing.', 19999, 15999, 'Buccal swab', 168, NULL),
 ('GEN-MTDNA', 'mtDNA Analysis', 'Genetics', 'Mitochondrial DNA analysis for maternal lineage testing.', 29999, 23999, 'Buccal swab', 240, NULL),
 ('GEN-LAUT',  'Legal/Transplant Autosomal Analysis', 'Legal DNA', 'Court-admissible DNA testing with chain of custody, for legal and transplant verification.', 24999, 19999, 'Blood', 168, NULL),
 ('GEN-LYSTR', 'Legal/Transplant YSTR Analysis', 'Legal DNA', 'Legal Y-chromosome testing with chain of custody.', 26999, 21599, 'Blood', 168, NULL),
 ('GEN-LMT',   'Legal/Transplant mtDNA Analysis', 'Legal DNA', 'Court-admissible mitochondrial DNA testing with chain of custody.', 29999, 23999, 'Blood', 240, NULL),
 ('GEN-PPNIPT','Prenatal Paternity + NIPT', 'Prenatal', 'Combined prenatal paternity and non-invasive prenatal testing (NIPT + NIPT Plus).', 119999, NULL, 'Blood', 240, NULL),
 ('GEN-WGS',   'Whole Genome Sequencing', 'Genetics', 'Complete DNA sequencing for comprehensive health and ancestry insights.', 29999, NULL, 'Blood', 504, NULL),
 ('GEN-INF',   'Infertility NGS Panel', 'Reproductive', 'Next-generation sequencing panel for genetic causes of infertility.', 24999, NULL, 'Blood', 336, NULL),
 ('GEN-CMA',   'Chromosomal Microarray (CMA)', 'Genetics', 'High-resolution analysis of chromosomal abnormalities and copy number variations.', 24999, NULL, 'Blood', 336, NULL),
 ('GEN-KARYO', 'Karyotyping (Husband & Wife)', 'Reproductive', 'Chromosome analysis for numerical and structural abnormalities.', 6999, NULL, 'Blood', 336, NULL),
 ('GEN-POC',   'POC by NGS', 'Reproductive', 'Product of conception testing by NGS to find causes of pregnancy loss.', 11999, NULL, 'Tissue', 336, NULL),
 ('GEN-EMB',   'Embryology by NGS', 'Reproductive', 'Preimplantation genetic testing of embryos using NGS.', 31999, NULL, 'Embryo biopsy', 336, NULL),
 ('GEN-HERED', 'Hereditary Panel', 'Genetics', 'Genetic screening for inherited conditions and hereditary diseases.', 34999, NULL, 'Blood', 504, NULL),
 ('GEN-MG72',  'Multigene Panel (72 Genes)', 'Genetics', 'Targeted analysis of 72 genes associated with genetic conditions.', 34999, NULL, 'Blood', 504, NULL),
 ('GEN-MG350', 'Multigene Panel (350 Genes)', 'Genetics', 'Extended multigene analysis covering 350 genes.', 64999, NULL, 'Blood', 504, NULL),
 ('GEN-MG1212','Multigene Panel (1212 Genes)', 'Genetics', 'The most extensive panel, covering 1212 genes.', 149999, NULL, 'Blood', 504, NULL),
 ('GEN-NIPT',  'NIPT', 'Prenatal', 'Non-invasive prenatal testing for chromosomal abnormalities during pregnancy.', 13999, NULL, 'Blood', 240, NULL);
