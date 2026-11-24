/**
 * Main Application Logic & Simple Router
 */

class App {
    constructor() {
        this.mainContent = document.getElementById('main-content');
        this.sidebar = document.getElementById('sidebar');
        this.logoutBtn = document.getElementById('logoutBtn');
        this.navLinks = document.querySelectorAll('.nav-link');

        this.init();
    }

    init() {
        // Listen for hash changes to simulate routing
        window.addEventListener('hashchange', () => this.handleRoute());

        // Setup Logout
        if (this.logoutBtn) {
            this.logoutBtn.addEventListener('click', () => this.logout());
        }

        // Initial route handling
        this.handleRoute();
    }

    async handleRoute() {
        let hash = window.location.hash.substring(1) || 'dashboard';

        // Simple Auth Guard
        const isAuthenticated = !!localStorage.getItem('jwt_token');
        if (!isAuthenticated && hash !== 'login') {
            window.location.hash = 'login';
            return;
        }

        if (isAuthenticated && hash === 'login') {
            window.location.hash = 'dashboard';
            return;
        }

        if (isAuthenticated && !this.userProfileLoaded) {
            await this.loadUserProfile();
        }

        // Role-based protection: non-admins cannot access members
        const role = localStorage.getItem('current_role');
        if (hash === 'members' && role !== 'Admin') {
            window.location.hash = 'dashboard';
            return;
        }

        this.updateNavigation(hash);
        this.loadView(hash);
    }

    async loadUserProfile() {
        try {
            const res = await api.get('/auth/me');
            if (res.success && res.data) {
                const u = res.data;
                this.userProfileLoaded = true;
                localStorage.setItem('current_username', u.username);
                localStorage.setItem('current_user_id', u.id);
                localStorage.setItem('current_role', u.role);

                const profileBlock = document.getElementById('currentUserProfile');
                if (profileBlock) {
                    profileBlock.style.display = 'flex';
                    document.getElementById('currentUserName').textContent = u.name;
                    document.getElementById('currentUserRole').textContent = u.role;
                    document.getElementById('currentUserInitials').textContent = u.name.charAt(0).toUpperCase();
                }

                // Show/hide admin-only elements in sidebar
                const adminElements = document.querySelectorAll('.admin-only');
                adminElements.forEach(el => {
                    if (u.role === 'Admin') {
                        el.style.display = 'flex';
                    } else {
                        el.style.display = 'none';
                    }
                });
            }
        } catch (e) {
            console.error("Failed to load user profile", e);
        }
    }

    updateNavigation(currentRoute) {
        // Toggle Sidebar visibility based on route
        if (currentRoute === 'login') {
            this.sidebar.classList.add('hidden');
            this.mainContent.classList.add('full-width');
        } else {
            this.sidebar.classList.remove('hidden');
            this.mainContent.classList.remove('full-width');
        }

        // Update active state on nav links
        this.navLinks.forEach(link => {
            if (link.dataset.route === currentRoute) {
                link.classList.add('active');
            } else {
                link.classList.remove('active');
            }
        });
    }

    async loadView(route) {
        try {
            // Fetch HTML template for the route
            const response = await fetch(`/pages/${route}.html`);
            if (!response.ok) throw new Error('View not found');

            const html = await response.text();
            this.mainContent.innerHTML = html;

            // Load specific script for the view if exists
            this.loadViewScript(route);

        } catch (error) {
            console.error('Error loading view:', error);
            this.mainContent.innerHTML = `<div class="card"><h2>404 - Not Found</h2><p>Could not load the requested page.</p></div>`;
        }
    }

    loadViewScript(route) {
        // Remove previously loaded module script to avoid conflicts
        const oldScript = document.getElementById('view-script');
        if (oldScript) {
            oldScript.remove();
        }

        // We check if the file exists dynamically or simply try to load it
        const script = document.createElement('script');
        script.id = 'view-script';
        // Add cache buster to ensure latest script is loaded in development
        script.src = `/js/modules/${route}.js?t=${new Date().getTime()}`;
        script.onerror = () => { console.log(`No specific JS for ${route}`); };
        document.body.appendChild(script);
    }

    logout() {
        localStorage.removeItem('jwt_token');
        localStorage.removeItem('current_username');
        localStorage.removeItem('current_user_id');
        localStorage.removeItem('current_role');
        this.userProfileLoaded = false;

        const profileBlock = document.getElementById('currentUserProfile');
        if (profileBlock) {
            profileBlock.style.display = 'none';
        }

        window.location.hash = 'login';
    }
}

// Initialize App on DOM Content Loaded
document.addEventListener('DOMContentLoaded', () => {
    window.app = new App();
});
