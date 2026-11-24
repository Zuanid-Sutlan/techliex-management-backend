(async function() {
    try {
        const response = await api.get('/dashboard/stats');
        if (response.success && response.data) {
            const stats = response.data;
            document.getElementById('stat-products').textContent = stats.productCount || 0;
            document.getElementById('stat-orders').textContent = stats.activeOrdersCount || 0;
            document.getElementById('stat-tracking').textContent = stats.pendingTrackingCount || 0;
            document.getElementById('stat-support').textContent = stats.unreadSupportCount || 0;

            // Format revenue
            const revenue = stats.totalEarnings || 0;
            document.getElementById('stat-revenue').textContent = '$' + revenue.toFixed(2);

            // Handle Admin Attention (Pending Warehouse Info)
            const role = localStorage.getItem('current_role');
            if (role === 'Admin' || role === 'Warehouse') {
                const attentionCard = document.getElementById('adminAttentionCard');
                if (attentionCard) {
                    attentionCard.style.display = 'block';
                    document.getElementById('stat-attention').textContent = stats.pendingWarehouseCount || 0;
                }
            }
        }
    } catch (error) {
        console.error("Failed to load dashboard stats:", error);
    }
})();