#!/usr/bin/env python3
import argparse, json, os, sys, pickle

def predict(model_dir, dataset_id, text):
    result = {"status": "error", "prediction": None, "probabilities": {}}
    try:
        model_path = os.path.join(model_dir, f"model_{dataset_id}.pkl")
        vec_path = os.path.join(model_dir, f"vectorizer_{dataset_id}.pkl")
        if not os.path.exists(model_path):
            raise FileNotFoundError(f"Modele introuvable. Lancez d abord l entrainement.")
        with open(model_path, 'rb') as f:
            model = pickle.load(f)
        with open(vec_path, 'rb') as f:
            vectorizer = pickle.load(f)
        X = vectorizer.transform([text])
        prediction = model.predict(X)[0]
        probas = model.predict_proba(X)[0]
        result["status"] = "success"
        result["prediction"] = prediction
        result["probabilities"] = {cls: round(float(p), 4) for cls, p in zip(model.classes_, probas)}
    except Exception as e:
        result["error"] = str(e)
        print(f"[ERROR] {e}", file=sys.stderr)
    print(json.dumps(result, ensure_ascii=False))
    return result["status"] == "success"

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--model', required=True)
    parser.add_argument('--dataset-id', required=True)
    parser.add_argument('--text', required=True)
    args = parser.parse_args()
    sys.exit(0 if predict(args.model, args.dataset_id, args.text) else 1)
