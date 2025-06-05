
from flask import Flask, request, jsonify
from datetime import datetime
import os
import json

app = Flask(__name__)

DATA_DIR = "data"
os.makedirs(DATA_DIR, exist_ok=True)

def get_date_key(date_str=None):
    if date_str:
        return date_str
    return datetime.now().strftime("%Y-%m-%d")

@app.route("/summary", methods=["POST"])
def receive_summary():
    data = request.json
    date_key = data.get("date") or get_date_key()
    filepath = os.path.join(DATA_DIR, f"{date_key}.json")
    with open(filepath, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    return {"status": "saved", "date": date_key}

@app.route("/summary/latest", methods=["GET"])
def get_latest_summary():
    files = sorted(os.listdir(DATA_DIR), reverse=True)[:2]
    result = {}
    for file in files:
        path = os.path.join(DATA_DIR, file)
        with open(path, encoding="utf-8") as f:
            content = json.load(f)
            summary = {}
            for meal in content.get("meals", []):
                name = meal.get("meal")
                nutrients = {"カロリー": 0, "たんぱく質": 0, "脂質": 0,
                             "炭水化物": 0, "食物繊維": 0, "塩分": 0}
                for food in meal.get("foods", []):
                    nutrients["カロリー"] += food.get("calories", 0)
                    nutrients["たんぱく質"] += food.get("protein", 0)
                    nutrients["脂質"] += food.get("fat", 0)
                    nutrients["炭水化物"] += food.get("carbs", 0)
                    nutrients["食物繊維"] += food.get("fiber", 0)
                    nutrients["塩分"] += food.get("salt", 0)
                summary[name] = {k: round(v, 1) for k, v in nutrients.items()}
            result[file.replace(".json", "")] = summary
    return jsonify(result)
