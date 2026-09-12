async function mentorChat() {
    const input = document.getElementById('mentor-input');
    const history = document.getElementById('chat-history');
    const query = input.value.trim();
    
    if (!query) return;
    
    // User message bubble
    const userBubble = document.createElement('div');
    userBubble.className = "flex justify-end mb-4";
    userBubble.innerHTML = `<div class="bg-blue-600 text-white p-4 rounded-2xl rounded-tr-none max-w-[80%] shadow-lg">${query}</div>`;
    history.appendChild(userBubble);
    input.value = '';
    history.scrollTop = history.scrollHeight;

    const system = "You are WiseWays AI Career Mentor. Use basic HTML like <b>, <ul>, <li>. Be encouraging and use emojis.";
    
    try {
        // Flask Server ko call
        const res = await fetch('http://localhost:5000/ask', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ query: `${system}\n\nStudent asks: ${query}` })
        });
        
        const data = await res.json();
        const aiMsg = data.response;
        
        const aiBubble = document.createElement('div');
        aiBubble.className = "flex justify-start mb-4";
        aiBubble.innerHTML = `<div class="bg-slate-700 text-slate-200 p-4 rounded-2xl rounded-tl-none max-w-[80%]">${aiMsg}</div>`;
        history.appendChild(aiBubble);
        
        speakText(aiMsg);
        
    } catch (e) {
        showToast("Error: app.py terminal mein run nahi ho raha!", "error");
    }
    history.scrollTop = history.scrollHeight;
}