(async function() {
    const saveBtn = document.getElementById('saveSettingsBtn');
    const themeSelect = document.getElementById('themeSelect');
    const overdueLimit = document.getElementById('overdueLimit');
    const message = document.getElementById('settingsMessage');

    // Load initial
    const currentTheme = localStorage.getItem('theme') || 'system';
    const currentLimit = localStorage.getItem('overdueLimit') || '7';

    if (themeSelect) themeSelect.value = currentTheme;
    if (overdueLimit) overdueLimit.value = currentLimit;

    if (saveBtn) {
        saveBtn.addEventListener('click', () => {
            localStorage.setItem('theme', themeSelect.value);
            localStorage.setItem('overdueLimit', overdueLimit.value);

            message.style.display = 'block';
            setTimeout(() => {
                message.style.display = 'none';
            }, 3000);
        });
    }

    // Admin Order Statuses Management
    const role = localStorage.getItem('current_role');
    const statusCard = document.getElementById('orderStatusesCard');
    const statusList = document.getElementById('statusList');
    const addStatusForm = document.getElementById('addStatusForm');

    if (role === 'Admin' && statusCard) {
        statusCard.style.display = 'block';
        await loadStatuses();
    }

    async function loadStatuses() {
        try {
            const res = await api.get('/statuses');
            if (res.success && res.data) {
                if (res.data.length === 0) {
                    statusList.innerHTML = '<p style="color: var(--text-secondary); font-size: 0.85rem;">No custom statuses configured.</p>';
                    return;
                }
                statusList.innerHTML = res.data.map(s => `
                    <div style="display: flex; justify-content: space-between; align-items: center; background: var(--background-color); padding: 0.5rem 0.75rem; border-radius: 6px; border: 1px solid var(--border-color);">
                        <span style="font-weight: 500;">${s.statusName}</span>
                        <button type="button" class="btn btn-outline delete-status-btn" data-id="${s.id}" style="padding: 0.15rem 0.4rem; font-size: 0.75rem; color: var(--danger-color); border-color: var(--danger-color);">Delete</button>
                    </div>
                `).join('');

                document.querySelectorAll('.delete-status-btn').forEach(btn => {
                    btn.addEventListener('click', async (e) => {
                        const id = e.target.dataset.id;
                        if (confirm('Are you sure you want to delete this status?')) {
                            try {
                                const delRes = await api.delete(`/statuses/${id}`);
                                if (delRes.success) {
                                    loadStatuses();
                                } else {
                                    alert(delRes.message || 'Failed to delete status');
                                }
                            } catch (err) {
                                alert('Error: ' + err.message);
                            }
                        }
                    });
                });
            }
        } catch (err) {
            console.error('Failed to load statuses', err);
        }
    }

    if (addStatusForm) {
        addStatusForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const statusName = document.getElementById('newStatusName').value.trim();
            if (!statusName) return;

            try {
                const addRes = await api.post('/statuses', { statusName });
                if (addRes.success) {
                    document.getElementById('newStatusName').value = '';
                    loadStatuses();
                } else {
                    alert(addRes.message || 'Failed to add status');
                }
            } catch (err) {
                alert('Error adding status: ' + err.message);
            }
        });
    }
})();