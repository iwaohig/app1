from flask import Flask, request, jsonify, send_from_directory
from flask_cors import CORS
import json
import os
from datetime import datetime

app = Flask(__name__, static_folder='static')
CORS(app)

DATA_DIR = "data"
os.makedirs(DATA_DIR, exist_ok=True)

@app.route("/")
def index():
    return send_from_directory('static', 'index.html')

@app.route("/save", methods=["POST"])
def save_data():
    data = request.json
    date = data.get("date")
    content = data.get("data")
    if not date or not content:
        return jsonify({"error": "Invalid data"}), 400
    with open(f"{DATA_DIR}/{date}.json", "w", encoding="utf-8") as f:
        json.dump(content, f, ensure_ascii=False, indent=2)
    return jsonify({"status": "ok"})

@app.route("/summary/<date>", methods=["GET"])
def get_data(date):
    file_path = f"{DATA_DIR}/{date}.json"
    if not os.path.exists(file_path):
        return jsonify({"error": "Data not found"}), 404
    with open(file_path, "r", encoding="utf-8") as f:
        content = json.load(f)
    return jsonify({"date": date, "data": content})
