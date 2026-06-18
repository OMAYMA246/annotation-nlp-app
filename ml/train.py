#!/usr/bin/env python3
import argparse, json, os, sys, pickle
from datetime import datetime
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, f1_score, classification_report, confusion_matrix
from sklearn.model_selection import train_test_split

def load_annotations(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        data = json.load(f)
    texts, labels = [], []
    for item in data:
        text = ' [SEP] '.join(item.get('textes', []))
        label = item.get('classe', '')
        if text and label:
            texts.append(text)
            labels.append(label)
    return texts, labels

def train(input_file, output_dir, dataset_id):
    os.makedirs(output_dir, exist_ok=True)
    result = {"dataset_id": dataset_id, "timestamp": datetime.now().isoformat(), "status": "error", "metrics": {}}
    try:
        print(f"[INFO] Chargement depuis {input_file}")
        texts, labels = load_annotations(input_file)
        if len(texts) < 4:
            raise ValueError(f"Pas assez d annotations ({len(texts)}). Minimum 4 requis.")
        print(f"[INFO] {len(texts)} exemples, classes: {set(labels)}")
        X_train, X_test, y_train, y_test = train_test_split(texts, labels, test_size=0.2, random_state=42)
        vectorizer = TfidfVectorizer(max_features=10000, ngram_range=(1,2), sublinear_tf=True)
        X_train_vec = vectorizer.fit_transform(X_train)
        X_test_vec = vectorizer.transform(X_test)
        print("[INFO] Entrainement Logistic Regression...")
        model = LogisticRegression(max_iter=1000, C=1.0)
        model.fit(X_train_vec, y_train)
        y_pred = model.predict(X_test_vec)
        accuracy = accuracy_score(y_test, y_pred)
        f1 = f1_score(y_test, y_pred, average='weighted', zero_division=0)
        cm = confusion_matrix(y_test, y_pred).tolist()
        classes = sorted(list(set(labels)))
        print(f"[RESULT] Accuracy: {accuracy:.4f}")
        print(f"[RESULT] F1-Score: {f1:.4f}")
        print(classification_report(y_test, y_pred, zero_division=0))
        with open(os.path.join(output_dir, f"model_{dataset_id}.pkl"), 'wb') as f:
            pickle.dump(model, f)
        with open(os.path.join(output_dir, f"vectorizer_{dataset_id}.pkl"), 'wb') as f:
            pickle.dump(vectorizer, f)
        result["status"] = "success"
        result["metrics"] = {
            "accuracy": round(accuracy, 4), "f1_score": round(f1, 4),
            "train_size": len(X_train), "test_size": len(X_test),
            "classes": classes, "confusion_matrix": cm,
            "classification_report": classification_report(y_test, y_pred, output_dict=True, zero_division=0)
        }
    except Exception as e:
        result["error"] = str(e)
        print(f"[ERROR] {e}", file=sys.stderr)
    with open(os.path.join(output_dir, f"result_{dataset_id}.json"), 'w', encoding='utf-8') as f:
        json.dump(result, f, ensure_ascii=False, indent=2)
    print(f"[INFO] Resultats sauvegardes")
    return result["status"] == "success"

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--input', required=True)
    parser.add_argument('--output', required=True)
    parser.add_argument('--dataset-id', required=True)
    args = parser.parse_args()
    sys.exit(0 if train(args.input, args.output, args.dataset_id) else 1)
