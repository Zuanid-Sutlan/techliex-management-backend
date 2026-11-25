/**
 * Optimized API wrapper for interacting with the Ktor backend.
 * Handles JWT token injection, intelligent in-memory GET caching, automatic cache invalidation on mutations, and smooth loading indicators.
 */
class ApiClient {
    constructor(baseURL) {
        this.baseURL = baseURL;
        this.cache = new Map(); // endpoint -> { data, timestamp }
        this.cacheTTL = 15000; // 15 seconds in-memory cache for ultra-smooth navigation
        this.activeRequests = 0;
    }

    getToken() {
        return localStorage.getItem('jwt_token');
    }

    showLoader() {
        this.activeRequests++;
        const loader = document.getElementById('globalLoader');
        if (loader) loader.classList.add('loading');
    }

    hideLoader() {
        this.activeRequests = Math.max(0, this.activeRequests - 1);
        if (this.activeRequests === 0) {
            const loader = document.getElementById('globalLoader');
            if (loader) loader.classList.remove('loading');
        }
    }

    async request(endpoint, options = {}) {
        const method = (options.method || 'GET').toUpperCase();
        const url = `${this.baseURL}${endpoint}`;

        // Intelligent GET Caching for lightning-fast user experience
        if (method === 'GET') {
            const cached = this.cache.get(endpoint);
            if (cached && (Date.now() - cached.timestamp < this.cacheTTL)) {
                return cached.data;
            }
        }

        const headers = {
            'Content-Type': 'application/json',
            ...options.headers
        };

        const token = this.getToken();
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const config = {
            ...options,
            headers
        };

        this.showLoader();
        try {
            const response = await fetch(url, config);

            // Handle 401 Unauthorized globally
            if (response.status === 401) {
                localStorage.removeItem('jwt_token');
                localStorage.removeItem('current_username');
                localStorage.removeItem('current_user_id');
                localStorage.removeItem('current_role');
                window.location.hash = '#login';
                throw new Error('Unauthorized');
            }

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || 'API request failed');
            }

            const result = await response.json();

            // Automatic Cache Invalidation on any data mutation (POST, PATCH, DELETE)
            if (method !== 'GET') {
                this.cache.clear(); // Wipes old cache so user instantly gets latest fresh data on next read
            } else {
                this.cache.set(endpoint, { data: result, timestamp: Date.now() });
            }

            return result;
        } catch (error) {
            console.error(`API Error on ${endpoint}:`, error);
            throw error;
        } finally {
            this.hideLoader();
        }
    }

    get(endpoint) {
        return this.request(endpoint, { method: 'GET' });
    }

    post(endpoint, data) {
        return this.request(endpoint, {
            method: 'POST',
            body: JSON.stringify(data)
        });
    }

    patch(endpoint, data) {
        return this.request(endpoint, {
            method: 'PATCH',
            body: JSON.stringify(data)
        });
    }

    delete(endpoint) {
        return this.request(endpoint, { method: 'DELETE' });
    }

    clearCache() {
        this.cache.clear();
    }
}

// Global instance (Use relative path so it works seamlessly on any host/domain)
const api = new ApiClient('/api/v1');
