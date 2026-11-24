(function() {
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
})();