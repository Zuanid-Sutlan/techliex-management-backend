(function() {
    const loginForm = document.getElementById('loginForm');
    const errorDiv = document.getElementById('loginError');

    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const username = document.getElementById('username').value;
            const password = document.getElementById('password').value;

            try {
                // Using the global api object defined in api.js
                const response = await api.post('/auth/login', { username, password });

                // Save token
                if (response.token) {
                    localStorage.setItem('jwt_token', response.token);
                    // Remember username
                    localStorage.setItem('last_username', username);
                    // Redirect to dashboard
                    window.location.hash = 'dashboard';
                }
            } catch (error) {
                errorDiv.textContent = error.message || 'Login failed. Please check your credentials.';
                errorDiv.style.display = 'block';
            }
        });

        // Pre-fill username if available
        const lastUser = localStorage.getItem('last_username');
        if (lastUser) {
            document.getElementById('username').value = lastUser;
            document.getElementById('password').focus();
        }
    }
})();
