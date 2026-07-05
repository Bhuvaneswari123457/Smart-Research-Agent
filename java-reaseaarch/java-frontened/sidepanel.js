document.addEventListener('DOMContentLoaded', () => {
    // Load existing notes from local storage
    chrome.storage.local.get(['researchNotes'], function(result)  {
        if (result.researchNotes) {
            document.getElementById('notes').value = result.researchNotes;
        }
    });

    // Add event listeners
    document.getElementById('summarizeButton').addEventListener('click', summarizeText);
    document.getElementById('saveNotesButton').addEventListener('click', saveNotes);
});

async function summarizeText() {
    try {
        const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
        
        // Execute script to get highlighted text
        const result = await chrome.scripting.executeScript({
            target: { tabId: tab.id },
            func: () => window.getSelection().toString()
        });

        const selectedText = result[0].result;
        if (!selectedText) {
            showResult("Please select some text first.");
            return;
        }

        // Call Backend Spring Boot Endpoint
        const response = await fetch('http://localhost:8080/api/research/process', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                content: selectedText,
                operation: 'summarize'
            })
        });

        if (!response.ok) {
            throw new Error(`API Error: ${response.status}`);
        }

        const text = await response.text();
        // Replace newlines with HTML line breaks
        const formattedText = text.replace(/\n/g, '<br>');
        showResult(formattedText);

    } catch (error) {
        showResult('Error:' +error.message);
    }
}

function saveNotes() {
    const notesValue = document.getElementById('notes').value;
    chrome.storage.local.set({ researchNotes: notesValue }, () => {
        alert('Notes saved successfully!');
    });
}

function showResult(content) {
    document.getElementById('results').innerHTML = `
        <div class="result-item">
            <div class="result-content">${content}</div>
        </div>
    `;
}