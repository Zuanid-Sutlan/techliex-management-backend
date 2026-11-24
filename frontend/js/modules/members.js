(async function() {
    const tbody = document.getElementById('membersTableBody');
    const modal = document.getElementById('addMemberModal');
    const addBtn = document.getElementById('addMemberBtn');
    const closeBtn = document.getElementById('closeAddMemberModal');
    const cancelBtn = document.getElementById('cancelAddMember');
    const form = document.getElementById('addMemberForm');
    const errorDiv = document.getElementById('memberError');

    async function loadMembers() {
        try {
            const response = await api.get('/users');

            if (response.success && response.data) {
                if (response.data.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;">No members found</td></tr>';
                    return;
                }

                const currentUser = localStorage.getItem('current_username');

                tbody.innerHTML = response.data.map(m => {
                    const isSelf = m.username === currentUser;
                    return `
                    <tr style="border-bottom: 1px solid var(--border-color);">
                        <td style="padding: 1rem 0; font-weight: bold;">${m.username} ${isSelf ? '<span style="font-size: 0.75rem; color: var(--text-secondary);">(You)</span>' : ''}</td>
                        <td style="padding: 1rem 0;">
                            <span style="background: #e0e7ff; color: #4338ca; padding: 0.25rem 0.5rem; border-radius: 4px; font-size: 0.875rem;">${m.role}</span>
                        </td>
                        <td style="padding: 1rem 0;">
                            <span style="color: ${m.isActive ? 'var(--success-color)' : 'var(--danger-color)'};">${m.isActive ? 'Active' : 'Inactive'}</span>
                        </td>
                        <td style="padding: 1rem 0; display: flex; gap: 0.5rem;">
                            <button class="btn btn-outline view-member-btn" data-username="${m.username}" style="padding: 0.25rem 0.5rem; font-size: 0.875rem;">View</button>
                            <button class="btn btn-outline edit-member-btn" data-username="${m.username}" data-active="${m.isActive}" data-self="${isSelf}" style="padding: 0.25rem 0.5rem; font-size: 0.875rem; color: var(--primary-color); border-color: var(--primary-color);">Edit Settings</button>
                            ${!isSelf ? `<button class="btn btn-outline delete-member-btn" data-username="${m.username}" style="padding: 0.25rem 0.5rem; font-size: 0.875rem; color: var(--danger-color); border-color: var(--danger-color);">Delete</button>` : ''}
                        </td>
                    </tr>
                `}).join('');

                // Attach event listeners to all newly rendered delete buttons
                document.querySelectorAll('.delete-member-btn').forEach(btn => {
                    btn.addEventListener('click', async (e) => {
                        const username = e.target.dataset.username;
                        if (confirm(`Are you sure you want to permanently delete member '${username}'?`)) {
                            try {
                                const delResponse = await api.delete(`/users/${username}/permanent`);
                                if (delResponse.success) {
                                    loadMembers();
                                } else {
                                    alert(delResponse.message || 'Failed to delete member.');
                                }
                            } catch (err) {
                                alert(err.message || 'Error deleting member.');
                            }
                        }
                    });
                });

                // Attach View buttons
                document.querySelectorAll('.view-member-btn').forEach(btn => {
                    btn.addEventListener('click', async (e) => {
                        const username = e.target.dataset.username;
                        try {
                            const res = await api.get(`/users/${username}`);
                            if (res.success && res.data) {
                                const u = res.data;
                                document.getElementById('viewMemberDetails').innerHTML = `
                                    <div style="margin-bottom: 1rem;"><strong>Username:</strong> ${u.username}</div>
                                    <div style="margin-bottom: 1rem;"><strong>Name:</strong> ${u.name}</div>
                                    <div style="margin-bottom: 1rem;"><strong>Role:</strong> ${u.role}</div>
                                    <div style="margin-bottom: 1rem;"><strong>Status:</strong> <span style="color: ${u.isActive ? 'var(--success-color)' : 'var(--danger-color)'}">${u.isActive ? 'Active' : 'Inactive'}</span></div>
                                    <div style="margin-bottom: 1rem;"><strong>Created At:</strong> ${new Date(u.createdAt).toLocaleString()}</div>
                                `;
                                document.getElementById('viewMemberModal').classList.remove('hidden');
                            }
                        } catch (err) {
                            alert('Failed to load user details: ' + err.message);
                        }
                    });
                });

                // Attach Edit buttons
                document.querySelectorAll('.edit-member-btn').forEach(btn => {
                    btn.addEventListener('click', async (e) => {
                        const username = e.target.dataset.username;
                        const isActive = e.target.dataset.active === 'true';
                        const isSelf = e.target.dataset.self === 'true';

                        document.getElementById('editMemberUsername').value = username;
                        document.getElementById('editMemberNewPassword').value = '';
                        document.getElementById('editMemberError').style.display = 'none';
                        document.getElementById('editMemberSuccess').style.display = 'none';

                        const badge = document.getElementById('currentStatusBadge');
                        const toggleBtn = document.getElementById('toggleStatusBtn');

                        badge.textContent = isActive ? 'Active' : 'Inactive';
                        badge.style.backgroundColor = isActive ? '#d1fae5' : '#fee2e2';
                        badge.style.color = isActive ? '#065f46' : '#991b1b';

                        toggleBtn.textContent = isActive ? 'Deactivate Account' : 'Activate Account';
                        toggleBtn.dataset.action = isActive ? 'deactivate' : 'activate';
                        toggleBtn.dataset.username = username;

                        // Disable toggle button if it's the logged-in admin trying to deactivate themselves
                        if (isSelf) {
                            toggleBtn.disabled = true;
                            toggleBtn.style.opacity = '0.5';
                            toggleBtn.style.cursor = 'not-allowed';
                            toggleBtn.title = "You cannot deactivate your own account";
                        } else {
                            toggleBtn.disabled = false;
                            toggleBtn.style.opacity = '1';
                            toggleBtn.style.cursor = 'pointer';
                            toggleBtn.title = "";
                        }

                        document.getElementById('editMemberModal').classList.remove('hidden');
                    });
                });

            }
        } catch (error) {
            console.error("Failed to load members:", error);
            tbody.innerHTML = '<tr><td colspan="4" style="text-align:center; color: red;">Failed to load members (Requires Admin)</td></tr>';
        }
    }

    // Modal behavior
    function openModal() { modal.classList.remove('hidden'); errorDiv.style.display = 'none'; }
    function closeModal() { modal.classList.add('hidden'); form.reset(); }

    if (addBtn) addBtn.addEventListener('click', openModal);
    if (closeBtn) closeBtn.addEventListener('click', closeModal);
    if (cancelBtn) cancelBtn.addEventListener('click', closeModal);

    // View Modal Handlers
    document.getElementById('closeViewMemberModal').addEventListener('click', () => {
        document.getElementById('viewMemberModal').classList.add('hidden');
    });
    document.getElementById('closeViewMemberBtn').addEventListener('click', () => {
        document.getElementById('viewMemberModal').classList.add('hidden');
    });

    // Edit Modal Handlers
    document.getElementById('closeEditMemberModal').addEventListener('click', () => {
        document.getElementById('editMemberModal').classList.add('hidden');
    });
    document.getElementById('cancelEditMember').addEventListener('click', () => {
        document.getElementById('editMemberModal').classList.add('hidden');
    });

    // Handle Status Toggle (Activate / Deactivate)
    document.getElementById('toggleStatusBtn').addEventListener('click', async (e) => {
        const action = e.target.dataset.action; // 'activate' or 'deactivate'
        const username = e.target.dataset.username;
        const errDiv = document.getElementById('editMemberError');
        const sucDiv = document.getElementById('editMemberSuccess');
        errDiv.style.display = 'none';
        sucDiv.style.display = 'none';

        try {
            let res;
            if (action === 'activate') {
                res = await api.patch(`/users/${username}/activate`);
            } else {
                res = await api.delete(`/users/${username}`); // Deactivate
            }

            if (res.success) {
                sucDiv.textContent = `Account successfully ${action}d!`;
                sucDiv.style.display = 'block';
                // Reload main list to reflect status
                loadMembers();

                // Update badge and button for next click without closing modal
                const isNowActive = (action === 'activate');
                const badge = document.getElementById('currentStatusBadge');
                badge.textContent = isNowActive ? 'Active' : 'Inactive';
                badge.style.backgroundColor = isNowActive ? '#d1fae5' : '#fee2e2';
                badge.style.color = isNowActive ? '#065f46' : '#991b1b';
                e.target.textContent = isNowActive ? 'Deactivate Account' : 'Activate Account';
                e.target.dataset.action = isNowActive ? 'deactivate' : 'activate';
            } else {
                throw new Error(res.message);
            }
        } catch (err) {
            errDiv.textContent = err.message || 'Error updating status';
            errDiv.style.display = 'block';
        }
    });

    // Handle Reset Password Submit
    document.getElementById('editMemberForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const username = document.getElementById('editMemberUsername').value;
        const newPassword = document.getElementById('editMemberNewPassword').value;
        const errDiv = document.getElementById('editMemberError');
        const sucDiv = document.getElementById('editMemberSuccess');

        errDiv.style.display = 'none';
        sucDiv.style.display = 'none';

        if (!newPassword || newPassword.trim() === '') {
            errDiv.textContent = 'Please enter a new password.';
            errDiv.style.display = 'block';
            return;
        }

        try {
            const res = await api.patch(`/users/${username}/reset-password`, { newPassword });
            if (res.success) {
                sucDiv.textContent = 'Password reset successfully!';
                sucDiv.style.display = 'block';
                document.getElementById('editMemberNewPassword').value = '';
            } else {
                throw new Error(res.message);
            }
        } catch (err) {
            errDiv.textContent = err.message || 'Error resetting password';
            errDiv.style.display = 'block';
        }
    });

    if (form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const payload = {
                username: document.getElementById('memberUsername').value,
                name: document.getElementById('memberName').value,
                password: document.getElementById('memberPassword').value,
                role: document.getElementById('memberRole').value
            };

            try {
                const response = await api.post('/users', payload);
                if (response.success) {
                    closeModal();
                    loadMembers();
                } else {
                    throw new Error(response.message || 'Failed to create member');
                }
            } catch (err) {
                errorDiv.textContent = err.message;
                errorDiv.style.display = 'block';
            }
        });
    }

    // Initial load
    loadMembers();
})();