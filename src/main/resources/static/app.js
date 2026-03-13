// API Configuration
const API_BASE = '/api';

// State Management
let currentTemplates = [];
let selectedTemplateId = null;

// Initial Load
document.addEventListener('DOMContentLoaded', () => {
    loadTemplates();
});

async function loadTemplates() {
    try {
        const response = await fetch(`${API_BASE}/templates`);
        currentTemplates = await response.json();
        renderTemplates(currentTemplates);
    } catch (error) {
        console.error('Failed to load templates:', error);
        document.getElementById('templates-grid').innerHTML = `
            <div class="glass-card" style="grid-column: 1/-1; text-align: center; color: #ef4444;">
                Failed to load templates. Please check your backend.
            </div>
        `;
    }
}

function renderTemplates(templates) {
    const grid = document.getElementById('templates-grid');
    if (templates.length === 0) {
        grid.innerHTML = `
            <div class="glass-card" style="grid-column: 1/-1; text-align: center; padding: 4rem;">
                <p style="margin-bottom: 1.5rem;">No templates found. Create your first one!</p>
                <a href="editor.html" class="btn btn-primary">Open Template Studio</a>
            </div>
        `;
        return;
    }

    grid.innerHTML = templates.map(t => `
        <div class="glass-card template-card" onclick="openGenModal(${t.id})">
            <div class="template-preview" style="height: 180px; background: rgba(0,0,0,0.2); border-radius: 1rem; margin-bottom: 1.5rem; display: flex; align-items: center; justify-content: center; font-size: 0.8rem; color: var(--text-muted); overflow: hidden;">
                <div style="transform: scale(0.2); transform-origin: center; width: 800px; color: black; background: white; padding: 20px;">
                    ${t.htmlContent}
                </div>
            </div>
            <h3 style="margin-bottom: 0.5rem;">${t.name}</h3>
            <p style="font-size: 0.9rem; color: var(--text-muted); line-height: 1.4;">${t.description || 'No description provided.'}</p>
        </div>
    `).join('');
}

function openGenModal(id) {
    selectedTemplateId = id;
    const template = currentTemplates.find(t => t.id === id);
    const placeholders = extractPlaceholders(template.htmlContent);
    
    document.getElementById('modal-title').innerText = `Generate: ${template.name}`;
    const formFields = document.getElementById('form-fields');
    
    if (placeholders.length === 0) {
        formFields.innerHTML = '<p style="color: var(--text-muted)">No dynamic fields found in this template.</p>';
    } else {
        formFields.innerHTML = placeholders.map(p => `
            <div class="form-group" style="margin-bottom: 1.5rem;">
                <label style="display: block; margin-bottom: 0.5rem; font-size: 0.9rem; color: var(--text-muted); text-transform: capitalize;">${p.replace(/_/g, ' ')}</label>
                <input type="text" name="${p}" placeholder="Enter ${p}" required style="width: 100%; padding: 0.75rem; border-radius: 0.5rem; background: rgba(255,255,255,0.05); border: 1px solid var(--card-border); color: white;">
            </div>
        `).join('');
    }
    
    document.getElementById('gen-modal').style.display = 'flex';
}

function closeModal() {
    document.getElementById('gen-modal').style.display = 'none';
}

function extractPlaceholders(html) {
    const matches = html.match(/\{\{([^}]+)\}\}/g);
    if (!matches) return [];
    return [...new Set(matches.map(m => m.replace(/\{\{|\}\}/g, '').trim()))];
}

// Form Submission
document.getElementById('gen-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const type = e.submitter.getAttribute('data-type');
    const formData = new FormData(e.target);
    const data = Object.fromEntries(formData.entries());
    
    const submitBtn = e.submitter;
    const originalText = submitBtn.innerText;
    submitBtn.innerText = 'Generating...';
    submitBtn.disabled = true;

    try {
        const response = await fetch(`${API_BASE}/documents/generate-single?templateId=${selectedTemplateId}&type=${type}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        
        const result = await response.json();
        if (result.success) {
            window.location.href = `${API_BASE}/documents/download/${result.data}`;
            closeModal();
        } else {
            alert('Generation failed: ' + result.message);
        }
    } catch (error) {
        console.error('Error:', error);
        alert('An error occurred during generation.');
    } finally {
        submitBtn.innerText = originalText;
        submitBtn.disabled = false;
    }
});

// Styles for Modal (Dynamic Inject)
const style = document.createElement('style');
style.innerHTML = `
    .modal {
        position: fixed;
        top: 0; left: 0; width: 100%; height: 100%;
        background: rgba(0,0,0,0.8);
        display: none;
        align-items: center;
        justify-content: center;
        z-index: 1000;
        backdrop-filter: blur(4px);
    }
    .modal-content {
        max-width: 500px;
        width: 90%;
        max-height: 90vh;
        overflow-y: auto;
    }
    .template-card { cursor: pointer; }
    .template-card:active { transform: scale(0.98); }
`;
document.head.appendChild(style);
