ORGANIC_TREATMENT_MAP = {
    "powdery mildew": {
        "organic_treatment": "Spray neem oil (5ml/litre) + potassium bicarbonate solution weekly.",
        "cultural_practice": "Improve airflow via pruning; avoid overhead irrigation in the evening.",
        "severity_if_untreated": "medium",
    },
    "leaf rust": {
        "organic_treatment": "Apply sulfur-based organic fungicide; remove and destroy infected leaves.",
        "cultural_practice": "Rotate with non-host crops next season; avoid nitrogen over-application.",
        "severity_if_untreated": "high",
    },
    "bacterial blight": {
        "organic_treatment": "Copper-based organic bactericide (Bordeaux mixture) at first sign.",
        "cultural_practice": "Use certified disease-free seed; avoid working fields when foliage is wet.",
        "severity_if_untreated": "high",
    },
    "aphid infestation": {
        "organic_treatment": "Release ladybird beetles (natural predator) or spray neem-based insecticide.",
        "cultural_practice": "Intercrop with marigold or mustard as a trap crop.",
        "severity_if_untreated": "medium",
    },
    "bollworm": {
        "organic_treatment": "Pheromone traps + neem seed kernel extract (NSKE 5%) spray.",
        "cultural_practice": "Deep summer ploughing to expose pupae to predators/heat.",
        "severity_if_untreated": "high",
    },
    "root rot": {
        "organic_treatment": "Apply Trichoderma viride bio-fungicide to soil around root zone.",
        "cultural_practice": "Improve field drainage; avoid waterlogging.",
        "severity_if_untreated": "high",
    },
}

DEFAULT_TREATMENT = {
    "organic_treatment": "Isolate affected plants, apply a broad-spectrum neem-oil spray, and monitor over 3-5 days.",
    "cultural_practice": "Consult local Krishi Vigyan Kendra (KVK) for on-ground confirmation.",
    "severity_if_untreated": "unknown",
}


def get_organic_treatment(disease_name: str):
    key = (disease_name or "").strip().lower()
    for known_disease, treatment in ORGANIC_TREATMENT_MAP.items():
        if known_disease in key or key in known_disease:
            return {"disease": disease_name, **treatment}
    return {"disease": disease_name, **DEFAULT_TREATMENT}