from flask import Flask, request, jsonify
from flask_cors import CORS
from openai import OpenAI
import json
import re

app = Flask(__name__)
CORS(app)

client = OpenAI(
  base_url = "https://integrate.api.nvidia.com/v1",
  api_key = "${NVIDIA_API_KEY:-}" # Sahi key check karo
)

@app.route('/ask', methods=['POST'])
def ask_ai():
    data = request.json
    query = data.get("query")
    
    try:
        completion = client.chat.completions.create(
    model="meta/llama-3.1-8b-instruct",
    messages=[{"role": "user", "content": query}],
    temperature=0,  # Isse AI "To the point" jawab deta hai
    max_tokens=2048 # token value increased then the api get fetch more convient data from the model and give more accurate answer
)
        
        response_text = completion.choices[0].message.content
        return jsonify({"response": response_text})
        
    except Exception as e:
        print(f"Error: {e}")
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(port=5000, debug=True)