(async function() {
    const container = document.getElementById('ordersContainer');
    const modal = document.getElementById('addOrderModal');
    const addBtn = document.getElementById('addOrderBtn');
    const closeBtn = document.getElementById('closeAddOrderModal');
    const cancelBtn = document.getElementById('cancelAddOrder');
    const form = document.getElementById('addOrderForm');
    const errorDiv = document.getElementById('orderError');

    const detailsModal = document.getElementById('orderDetailsServiceModal');
    const closeDetailsModalBtn = document.getElementById('closeOrderDetailsModal');
    const closeDetailsBtn = document.getElementById('closeOrderDetailsBtn');
    const detailsBody = document.getElementById('orderDetailsServiceBody');
    const detailOrderNumber = document.getElementById('detailOrderNumber');

    const searchInput = document.getElementById('orderSearchInput');
    const userFilterSelect = document.getElementById('orderUserFilter');

    const userRole = localStorage.getItem('current_role') || 'Member';
    let allOrders = [];
    let currentFilter = 'all';
    let searchTerm = '';
    let selectedUserFilter = 'all';
    let huntedProductsCatalog = [];

    // Setup User Filter for Admins
    if (userRole === 'Admin' && userFilterSelect) {
        userFilterSelect.style.display = 'block';
        try {
            const uRes = await api.get('/users');
            if (uRes.success && uRes.data) {
                userFilterSelect.innerHTML = '<option value="all">All Members / Buyers</option>' +
                    uRes.data.map(u => `<option value="${u.name}">${u.name} (${u.username})</option>`).join('');
            }
        } catch (e) {
            console.error("Failed to load users for filter", e);
        }

        userFilterSelect.addEventListener('change', (e) => {
            selectedUserFilter = e.target.value;
            renderOrders();
        });
    }

    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            searchTerm = e.target.value.toLowerCase().trim();
            renderOrders();
        });
    }

    // Filter tab clicks
    document.querySelectorAll('.order-tab').forEach(tab => {
        tab.addEventListener('click', (e) => {
            document.querySelectorAll('.order-tab').forEach(t => {
                t.style.fontWeight = 'normal';
                t.style.color = 'var(--text-secondary)';
                t.style.borderBottom = 'none';
            });
            e.target.style.fontWeight = 'bold';
            e.target.style.color = 'var(--primary-color)';
            e.target.style.borderBottom = '2px solid var(--primary-color)';
            e.target.style.paddingBottom = '0.25rem';

            currentFilter = e.target.dataset.filter;
            renderOrders();
        });
    });

    async function loadOrders() {
        try {
            const res = await api.get('/orders');
            if (res.success && res.data) {
                allOrders = res.data;
                renderOrders();
            }
        } catch (error) {
            console.error("Failed to load orders:", error);
            container.innerHTML = '<p style="color: red; text-align: center; padding: 2rem;">Failed to load orders</p>';
        }
    }

    async function loadCatalog() {
        try {
            const res = await api.get('/products');
            if (res.success && res.data) {
                huntedProductsCatalog = res.data;
            }
        } catch (e) {
            console.error("Failed to load product catalog for orders", e);
        }
    }

    function renderOrders() {
        let filtered = allOrders;

        // Status tab filter
        if (currentFilter !== 'all') {
            filtered = filtered.filter(o => o.status === currentFilter);
        }

        // User filter
        if (selectedUserFilter !== 'all') {
            filtered = filtered.filter(o => o.userName === selectedUserFilter || o.userId === selectedUserFilter);
        }

        // Search term filter
        if (searchTerm) {
            filtered = filtered.filter(o =>
                (o.orderNumber && o.orderNumber.toLowerCase().includes(searchTerm)) ||
                (o.productName && o.productName.toLowerCase().includes(searchTerm)) ||
                (o.userName && o.userName.toLowerCase().includes(searchTerm))
            );
        }

        if (filtered.length === 0) {
            container.innerHTML = `
                <div style="text-align: center; padding: 3rem; color: var(--text-secondary);">
                    <i class="fas fa-inbox" style="font-size: 3rem; margin-bottom: 1rem; opacity: 0.5;"></i>
                    <p>No orders found matching your criteria.</p>
                </div>
            `;
            return;
        }

        // Group by orderNumber for Master Order Cards
        const grouped = {};
        filtered.forEach(o => {
            const num = o.orderNumber || ('ORD-' + o.id);
            if (!grouped[num]) grouped[num] = [];
            grouped[num].push(o);
        });

        // Smart Sorting:
        // Priority 1: Orders needing attention (Refund Requested or Active without tracking ID)
        // Priority 2: Active / Shipped / Other
        // Priority 3: Completed (goes to the bottom)
        const orderEntries = Object.entries(grouped);
        orderEntries.sort((a, b) => {
            const itemsA = a[1];
            const itemsB = b[1];
            const firstA = itemsA[0];
            const firstB = itemsB[0];

            const isCompletedA = firstA.status === 'Completed';
            const isCompletedB = firstB.status === 'Completed';

            if (isCompletedA && !isCompletedB) return 1; // A goes down
            if (!isCompletedA && isCompletedB) return -1; // B goes down

            const unreadA = (userRole === 'Admin' || userRole === 'Warehouse') ? firstA.hasUnreadByAdmin : firstA.hasUnreadByUser;
            const unreadB = (userRole === 'Admin' || userRole === 'Warehouse') ? firstB.hasUnreadByAdmin : firstB.hasUnreadByUser;

            if (unreadA && !unreadB) return -1;
            if (!unreadA && unreadB) return 1;

            const needsAttentionA = firstA.status === 'Refund Requested' || (firstA.status === 'Active' && !firstA.trackId);
            const needsAttentionB = firstB.status === 'Refund Requested' || (firstB.status === 'Active' && !firstB.trackId);

            if (needsAttentionA && !needsAttentionB) return -1; // A comes top
            if (!needsAttentionA && needsAttentionB) return 1; // B comes top

            return 0;
        });

        let html = '<div style="display: flex; flex-direction: column; gap: 1rem;">';
        for (const [orderNumber, items] of orderEntries) {
            const first = items[0];
            const hasUnread = (userRole === 'Admin' || userRole === 'Warehouse') ? first.hasUnreadByAdmin : first.hasUnreadByUser;
            const needsAttention = first.status === 'Refund Requested' || (first.status === 'Active' && !first.trackId);

            html += `
                <div class="card" style="margin-bottom: 0; border-left: 4px solid ${hasUnread ? 'var(--warning-color)' : (needsAttention ? 'var(--danger-color)' : 'var(--primary-color)')};">
                    <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.75rem; border-bottom: 1px solid var(--border-color); padding-bottom: 0.5rem;">
                        <div>
                            <h3 style="color: var(--text-primary); font-size: 1.05rem; display: flex; align-items: center; gap: 10px;">
                                ${orderNumber}
                                <span style="background: #e0e7ff; color: #4338ca; padding: 0.15rem 0.4rem; border-radius: 4px; font-size: 0.75rem;">${first.status}</span>
                                ${hasUnread ? '<span style="background: #fef08a; color: #854d0e; padding: 0.15rem 0.4rem; border-radius: 4px; font-size: 0.75rem; font-weight: bold;"><i class="fas fa-envelope"></i> Unread Message</span>' : ''}
                                ${needsAttention && !hasUnread ? '<span style="background: #fee2e2; color: #991b1b; padding: 0.15rem 0.4rem; border-radius: 4px; font-size: 0.75rem;">Needs Attention</span>' : ''}
                            </h3>
                            <p style="font-size: 0.8rem; color: var(--text-secondary); margin-top: 2px;">Buyer: <strong style="color: var(--text-primary);">${first.userName || 'Member'}</strong> | Date: ${first.orderDate || 'N/A'} | Items: ${items.length}</p>
                        </div>
                        <button class="btn btn-outline view-order-btn" data-id="${first.id}" style="padding: 0.25rem 0.6rem; font-size: 0.8rem;">Manage</button>
                    </div>
                    <div style="display: flex; flex-direction: column; gap: 0.4rem;">
                        ${items.map(item => `
                            <div style="display: flex; align-items: center; gap: 0.75rem; background: var(--background-color); padding: 0.4rem 0.6rem; border-radius: 6px;">
                                <img src="${item.productImage || 'https://via.placeholder.com/40'}" alt="${item.productName}" onclick="window.openImageViewer('${item.productImage}')" style="width: 32px; height: 32px; border-radius: 4px; object-fit: cover; cursor: zoom-in;" title="Click to view full size">
                                <div style="flex: 1; font-size: 0.9rem;">
                                    <strong>${item.productName}</strong> (Qty: ${item.quantity})
                                    ${item.note ? `<small style="color: var(--text-secondary); display: block;">${item.note}</small>` : ''}
                                </div>
                                <div style="font-weight: bold; font-size: 0.9rem; color: var(--text-primary);">
                                    $${(item.listingPrice * item.quantity).toFixed(2)}
                                </div>
                            </div>
                        `).join('')}
                    </div>
                </div>
            `;
        }
        html += '</div>';
        container.innerHTML = html;

        // Attach listeners
        document.querySelectorAll('.view-order-btn').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                const orderId = e.target.dataset.id;
                await openOrderDetails(orderId);
            });
        });
    }

    // Modal behavior for Add Order
    function openModal() {
        modal.classList.remove('hidden');
        errorDiv.style.display = 'none';
        document.getElementById('orderNumber').value = 'ORD-' + Math.floor(100000 + Math.random() * 900000);
        document.getElementById('orderDate').valueAsDate = new Date();
        document.getElementById('orderItemsList').innerHTML = '';
        addCartItemRow();
    }

    function closeModal() {
        modal.classList.add('hidden');
        form.reset();
        const previewContainer = document.getElementById('orderImgPreviewContainer');
        const dropzone = document.getElementById('orderDropzone');
        const screenshotHidden = document.getElementById('orderPaymentScreenshot');
        if (previewContainer) previewContainer.classList.add('hidden');
        if (dropzone) dropzone.style.display = 'block';
        if (screenshotHidden) screenshotHidden.value = '';
    }

    if (addBtn) addBtn.addEventListener('click', openModal);
    if (closeBtn) closeBtn.addEventListener('click', closeModal);
    if (cancelBtn) cancelBtn.addEventListener('click', closeModal);

    // Cart Items Builder
    const addItemBtn = document.getElementById('addItemBtn');
    if (addItemBtn) {
        addItemBtn.addEventListener('click', () => addCartItemRow());
    }

    function addCartItemRow() {
        const list = document.getElementById('orderItemsList');
        const rowId = 'cart_row_' + Date.now();
        const row = document.createElement('div');
        row.className = 'cart-item-row';
        row.id = rowId;
        row.style.cssText = 'background: var(--background-color); padding: 0.75rem; border-radius: 8px; border: 1px solid var(--border-color); display: flex; flex-direction: column; gap: 0.5rem;';

        row.innerHTML = `
            <div style="display: flex; justify-content: space-between; align-items: center;">
                <strong>Product Item</strong>
                <button type="button" class="btn btn-outline" style="padding: 0.1rem 0.3rem; color: var(--danger-color); font-size: 0.75rem;" onclick="document.getElementById('${rowId}').remove()">Remove</button>
            </div>
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 0.5rem;">
                <div>
                    <label style="font-size: 0.75rem; color: var(--text-secondary);">Source Type</label>
                    <select class="form-control item-source-type" onchange="toggleSourceType('${rowId}')" style="padding: 0.3rem;">
                        <option value="hunted">Hunted Catalog</option>
                        <option value="custom">Custom Product (Manual)</option>
                    </select>
                </div>
                <div class="hunted-product-group">
                    <label style="font-size: 0.75rem; color: var(--text-secondary);">Select Hunted Product</label>
                    <select class="form-control item-hunted-id" style="padding: 0.3rem;">
                        ${huntedProductsCatalog.map(p => `<option value="${p.id}" data-title="${p.title}" data-img="${p.productImageUrl}" data-price="${p.sourcePrice}">${p.title} ($${p.sourcePrice})</option>`).join('')}
                    </select>
                </div>
                <div class="custom-product-group" style="display: none; grid-column: span 2; flex-direction: column; gap: 0.5rem; background: var(--surface-color); padding: 0.75rem; border-radius: 6px; border: 1px dashed var(--border-color);">
                    <div>
                        <label style="font-size: 0.75rem; color: var(--text-secondary); font-weight: bold;">Custom Product Title / Name</label>
                        <input type="text" class="form-control item-custom-name" placeholder="e.g. Custom Mechanical Keyboard" style="padding: 0.4rem;">
                    </div>
                    <div>
                        <label style="font-size: 0.75rem; color: var(--text-secondary); font-weight: bold;">Product Image (Drag & Drop, Device Browse, or URL)</label>
                        <div class="dropzone" style="padding: 0.5rem; margin-top: 2px;" onclick="document.getElementById('file_${rowId}').click()" ondragover="event.preventDefault(); this.classList.add('dragover');" ondragleave="this.classList.remove('dragover')" ondrop="event.preventDefault(); this.classList.remove('dragover'); handleCustomProductDrop(event, '${rowId}')">
                            <p style="font-size: 0.75rem; color: var(--text-secondary); margin: 0;"><i class="fas fa-cloud-upload-alt" style="color: var(--primary-color);"></i> Drag & drop image here, or <span style="color: var(--primary-color);">browse device</span></p>
                            <input type="file" id="file_${rowId}" accept="image/*" style="display: none;" onchange="handleCustomProductFile(this, '${rowId}')">
                        </div>
                        <div style="display: flex; gap: 0.5rem; align-items: center; margin-top: 0.4rem;">
                            <input type="url" class="form-control item-custom-img" placeholder="Image URL / Base64" style="flex: 1; padding: 0.3rem; font-size: 0.8rem;">
                            <img id="preview_${rowId}" class="dropzone-preview hidden" src="" alt="Preview" style="width: 32px; height: 32px; border-radius: 4px; object-fit: cover;">
                        </div>
                    </div>
                    <div>
                        <label style="font-size: 0.75rem; color: var(--text-secondary); font-weight: bold;">Source Price ($)</label>
                        <input type="number" step="0.01" class="form-control item-custom-price" placeholder="0.00" style="padding: 0.4rem;">
                    </div>
                </div>
            </div>
            <div style="display: grid; grid-template-columns: 1fr 1fr 2fr; gap: 0.5rem;">
                <div>
                    <label style="font-size: 0.75rem; color: var(--text-secondary);">Quantity</label>
                    <input type="number" class="form-control item-qty" value="1" min="1" required style="padding: 0.3rem;">
                </div>
                <div>
                    <label style="font-size: 0.75rem; color: var(--text-secondary);">Listing Price ($)</label>
                    <input type="number" step="0.01" class="form-control item-price" value="0.00" required style="padding: 0.3rem;">
                </div>
                <div>
                    <label style="font-size: 0.75rem; color: var(--text-secondary);">Variation Note</label>
                    <input type="text" class="form-control item-note" placeholder="e.g. Black, Size L" style="padding: 0.3rem;">
                </div>
            </div>
        `;
        list.appendChild(row);
    }

    window.handleCustomProductFile = async function(input, rowId) {
        if (input.files && input.files[0]) {
            try {
                const base64 = await fileToBase64(input.files[0]);
                const row = document.getElementById(rowId);
                row.querySelector('.item-custom-img').value = base64;
                const prev = document.getElementById(`preview_${rowId}`);
                if (prev) {
                    prev.src = base64;
                    prev.classList.remove('hidden');
                }
            } catch (e) {
                console.error("Failed to parse custom product image", e);
            }
        }
    };

    window.handleCustomProductDrop = async function(event, rowId) {
        if (event.dataTransfer.files && event.dataTransfer.files[0]) {
            try {
                const base64 = await fileToBase64(event.dataTransfer.files[0]);
                const row = document.getElementById(rowId);
                row.querySelector('.item-custom-img').value = base64;
                const prev = document.getElementById(`preview_${rowId}`);
                if (prev) {
                    prev.src = base64;
                    prev.classList.remove('hidden');
                }
            } catch (e) {
                console.error("Failed to parse dropped custom product image", e);
            }
        }
    };

    window.toggleSourceType = function(rowId) {
        const row = document.getElementById(rowId);
        const type = row.querySelector('.item-source-type').value;
        const huntedGroup = row.querySelector('.hunted-product-group');
        const customGroup = row.querySelector('.custom-product-group');
        if (type === 'custom') {
            huntedGroup.style.display = 'none';
            customGroup.style.display = 'flex';
        } else {
            huntedGroup.style.display = 'block';
            customGroup.style.display = 'none';
        }
    };

    // Dropzone setup for orders payment proof
    const dropzone = document.getElementById('orderDropzone');
    const fileInput = document.getElementById('orderImgFile');
    const previewContainer = document.getElementById('orderImgPreviewContainer');
    const previewImg = document.getElementById('orderImgPreview');
    const fileNameSpan = document.getElementById('orderImgFileName');
    const removeImgBtn = document.getElementById('removeOrderImg');
    const screenshotHidden = document.getElementById('orderPaymentScreenshot');

    if (dropzone && fileInput) {
        dropzone.addEventListener('click', () => fileInput.click());
        dropzone.addEventListener('dragover', (e) => { e.preventDefault(); dropzone.classList.add('dragover'); });
        dropzone.addEventListener('dragleave', () => { dropzone.classList.remove('dragover'); });
        dropzone.addEventListener('drop', async (e) => {
            e.preventDefault();
            dropzone.classList.remove('dragover');
            if (e.dataTransfer.files && e.dataTransfer.files[0]) {
                await handleImageFile(e.dataTransfer.files[0]);
            }
        });
        fileInput.addEventListener('change', async (e) => {
            if (e.target.files && e.target.files[0]) {
                await handleImageFile(e.target.files[0]);
            }
        });
        removeImgBtn.addEventListener('click', () => {
            screenshotHidden.value = '';
            fileInput.value = '';
            previewImg.src = '';
            previewContainer.classList.add('hidden');
            dropzone.style.display = 'block';
        });
    }

    async function handleImageFile(file) {
        if (!file.type.startsWith('image/')) {
            alert('Please select a valid image file.');
            return;
        }
        try {
            const base64String = await fileToBase64(file);
            screenshotHidden.value = base64String;
            previewImg.src = base64String;
            fileNameSpan.textContent = file.name;
            dropzone.style.display = 'none';
            previewContainer.classList.remove('hidden');
        } catch (err) {
            console.error('Failed to convert payment screenshot to Base64', err);
        }
    }

    function fileToBase64(file) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.readAsDataURL(file);
            reader.onload = () => resolve(reader.result);
            reader.onerror = error => reject(error);
        });
    }

    if (form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const rows = document.querySelectorAll('.cart-item-row');
            if (rows.length === 0) {
                errorDiv.textContent = 'Please add at least one product item.';
                errorDiv.style.display = 'block';
                return;
            }

            const items = [];
            rows.forEach(r => {
                const sourceType = r.querySelector('.item-source-type').value;
                const qty = parseInt(r.querySelector('.item-qty').value) || 1;
                const price = parseFloat(r.querySelector('.item-price').value) || 0;
                const note = r.querySelector('.item-note').value;

                if (sourceType === 'hunted') {
                    const select = r.querySelector('.item-hunted-id');
                    const opt = select.options[select.selectedIndex];
                    if (opt) {
                        items.push({
                            productId: parseInt(opt.value),
                            productName: opt.dataset.title,
                            productImage: opt.dataset.img,
                            productSourcePrice: parseFloat(opt.dataset.price),
                            quantity: qty,
                            listingPrice: price,
                            note: note
                        });
                    }
                } else {
                    const name = r.querySelector('.item-custom-name').value;
                    const img = r.querySelector('.item-custom-img').value;
                    const cPrice = parseFloat(r.querySelector('.item-custom-price').value) || 0;
                    items.push({
                        productName: name,
                        productImage: img || 'https://via.placeholder.com/150',
                        productSourcePrice: cPrice,
                        quantity: qty,
                        listingPrice: price,
                        note: note
                    });
                }
            });

            const payload = {
                orderNumber: document.getElementById('orderNumber').value,
                orderDate: document.getElementById('orderDate').value,
                address: document.getElementById('orderAddress').value,
                paymentScreenshot: document.getElementById('orderPaymentScreenshot').value || null,
                items: items
            };

            try {
                const response = await api.post('/orders', payload);
                if (response.success) {
                    closeModal();
                    loadOrders();
                } else {
                    throw new Error(response.message || 'Failed to place orders');
                }
            } catch (err) {
                errorDiv.textContent = err.message;
                errorDiv.style.display = 'block';
            }
        });
    }

    // Order Details, Logistics & Chat Console
    async function openOrderDetails(orderId) {
        try {
            const res = await api.get(`/orders/${orderId}`);
            if (res.success && res.data) {
                const o = res.data;
                detailOrderNumber.textContent = `Order Details: ${o.orderNumber || ('ORD-' + o.id)}`;

                let allStatuses = [];
                try {
                    const stRes = await api.get('/statuses');
                    if (stRes.success && stRes.data) allStatuses = stRes.data;
                } catch (e) {
                    console.error("Failed to load statuses", e);
                }

                let html = `
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; background: var(--background-color); padding: 1rem; border-radius: 8px; border: 1px solid var(--border-color);">
                        <div><strong>Created By (Member):</strong> <span style="color: var(--primary-color); font-weight: bold;">${o.userName || 'Member'}</span></div>
                        <div><strong>Status:</strong> <span style="color: var(--primary-color); font-weight: bold;">${o.status}</span></div>
                        <div><strong>Order Date:</strong> ${o.orderDate || 'N/A'}</div>
                        <div style="grid-column: span 2;"><strong>Shipping Address:</strong> ${o.address}</div>
                        ${o.trackId ? `<div><strong>Courier:</strong> ${o.company}</div><div><strong>Tracking ID:</strong> ${o.trackId}</div>` : ''}
                    </div>

                    <div>
                        <h4 style="margin-bottom: 0.5rem; color: var(--primary-color);">Product Items</h4>
                        <div style="display: flex; flex-direction: column; gap: 0.5rem;">
                            <div style="display: flex; align-items: center; gap: 1rem; background: var(--surface-color); padding: 0.75rem; border-radius: 6px; border: 1px solid var(--border-color);">
                                <img src="${o.productImage}" alt="${o.productName}" onclick="window.openImageViewer('${o.productImage}')" style="width: 50px; height: 50px; border-radius: 6px; object-fit: cover; cursor: zoom-in;" title="Click to view full size">
                                <div style="flex: 1;">
                                    <strong>${o.productName}</strong> (Qty: ${o.quantity})
                                    ${o.note ? `<br><small style="color: var(--text-secondary);">${o.note}</small>` : ''}
                                </div>
                                <div style="font-weight: bold;">$${(o.listingPrice * o.quantity).toFixed(2)}</div>
                            </div>
                        </div>
                    </div>

                    ${o.paymentScreenshot ? `
                        <div>
                            <strong>Payment Proof:</strong><br>
                            <img src="${o.paymentScreenshot}" alt="Payment Proof" onclick="window.openImageViewer('${o.paymentScreenshot}')" style="max-width: 150px; max-height: 100px; border-radius: 6px; margin-top: 0.5rem; border: 1px solid var(--border-color); cursor: zoom-in;" title="Click to view full size">
                        </div>
                    ` : ''}
                `;

                if (userRole === 'Admin') {
                    html += `
                        <div style="background: var(--background-color); padding: 1rem; border-radius: 8px; border: 1px solid var(--border-color);">
                            <h4 style="margin-bottom: 0.75rem; color: var(--primary-color);"><i class="fas fa-tag"></i> Update Custom Status</h4>
                            <form id="statusUpdateForm" style="display: flex; gap: 0.75rem; align-items: flex-end;">
                                <div style="flex: 1;">
                                    <label style="font-size: 0.8rem; color: var(--text-secondary);">Select Status</label>
                                    <select id="customStatusSelect" class="form-control">
                                        ${allStatuses.map(s => `<option value="${s.statusName}" ${s.statusName === o.status ? 'selected' : ''}>${s.statusName}</option>`).join('')}
                                    </select>
                                </div>
                                <button type="submit" class="btn btn-primary" style="padding: 0.5rem 1rem;">Update Status</button>
                            </form>
                        </div>
                    `;
                }

                if (userRole === 'Admin' || userRole === 'Warehouse') {
                    html += `
                        <div style="background: var(--background-color); padding: 1rem; border-radius: 8px; border: 1px solid var(--border-color);">
                            <h4 style="margin-bottom: 0.75rem; color: var(--primary-color);"><i class="fas fa-shipping-fast"></i> Update Tracking & Processing</h4>
                            <form id="logisticsForm" style="display: flex; gap: 0.75rem; align-items: flex-end;">
                                <div style="flex: 1;">
                                    <label style="font-size: 0.8rem; color: var(--text-secondary);">Courier Company</label>
                                    <input type="text" id="logisticsCompany" class="form-control" placeholder="e.g. FedEx, DHL" value="${o.company || ''}" required>
                                </div>
                                <div style="flex: 1;">
                                    <label style="font-size: 0.8rem; color: var(--text-secondary);">Tracking ID</label>
                                    <input type="text" id="logisticsTrackId" class="form-control" placeholder="Tracking Number" value="${o.trackId || ''}" required>
                                </div>
                                <button type="submit" class="btn btn-primary" style="padding: 0.5rem 1rem;">Update Tracking & Processing</button>
                            </form>
                            ${o.status !== 'Completed' ? `
                                <button type="button" id="completeOrderBtn" class="btn btn-outline" style="margin-top: 0.75rem; width: 100%; border-color: var(--success-color); color: var(--success-color);">Mark Order as Completed</button>
                            ` : ''}
                        </div>
                    `;
                }

                html += `
                    <div style="background: var(--background-color); padding: 1rem; border-radius: 8px; border: 1px solid var(--border-color); display: flex; flex-direction: column; gap: 1rem;">
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                            <h4 style="color: var(--primary-color);"><i class="fas fa-comments"></i> Refund & Support Chat</h4>
                            ${userRole === 'Admin' ? `<button type="button" id="openRefundConvBtn" class="btn btn-outline" style="padding: 0.2rem 0.5rem; font-size: 0.75rem;">Open Refund Thread</button>` : ''}
                        </div>
                        <div id="chatMessagesContainer" style="max-height: 200px; overflow-y: auto; background: var(--surface-color); padding: 0.75rem; border: 1px solid var(--border-color); border-radius: 6px; display: flex; flex-direction: column; gap: 0.5rem;">
                            <p style="font-size: 0.85rem; color: var(--text-secondary); text-align: center;">Loading messages...</p>
                        </div>
                        <form id="chatMessageForm" style="display: flex; flex-direction: column; gap: 0.5rem;">
                            <div id="chatImagePreviewContainer" style="display: none; align-items: center; gap: 0.5rem;">
                                <img id="chatImagePreview" src="" style="height: 50px; border-radius: 4px; border: 1px solid var(--border-color); object-fit: cover;">
                                <button type="button" id="chatRemoveImageBtn" class="btn btn-outline" style="padding: 0.1rem 0.3rem; font-size: 0.75rem; color: var(--danger-color); border-color: transparent;">&times;</button>
                            </div>
                            <div style="display: flex; gap: 0.5rem;">
                                <label for="chatImageInput" class="btn btn-outline" style="padding: 0.4rem 0.6rem; cursor: pointer; color: var(--text-secondary);" title="Attach Image">
                                    <i class="fas fa-image"></i>
                                    <input type="file" id="chatImageInput" accept="image/*" style="display: none;">
                                </label>
                                <input type="text" id="chatInputText" class="form-control" placeholder="Type support message..." required style="flex: 1;">
                                <button type="submit" class="btn btn-primary">Send</button>
                            </div>
                        </form>
                    </div>
                `;

                if (userRole === 'Admin') {
                    html += `
                        <div style="border-top: 1px solid var(--border-color); padding-top: 1rem; display: flex; justify-content: flex-end;">
                            <button type="button" id="permanentlyDeleteOrderBtn" class="btn btn-outline" style="color: var(--danger-color); border-color: var(--danger-color);"><i class="fas fa-trash-alt"></i> Permanently Delete Order</button>
                        </div>
                    `;
                }

                detailsBody.innerHTML = html;
                detailsModal.classList.remove('hidden');

                await loadChatMessages(o.id);

                const statusForm = document.getElementById('statusUpdateForm');
                if (statusForm) {
                    statusForm.addEventListener('submit', async (evt) => {
                        evt.preventDefault();
                        const newStatus = document.getElementById('customStatusSelect').value;
                        try {
                            const sRes = await api.patch(`/orders/${o.id}/status`, { status: newStatus });
                            if (sRes.success) {
                                alert('Order status updated successfully!');
                                detailsModal.classList.add('hidden');
                                loadOrders();
                            } else {
                                alert(sRes.message || 'Failed to update status');
                            }
                        } catch (err) {
                            alert('Error: ' + err.message);
                        }
                    });
                }

                const logisticsForm = document.getElementById('logisticsForm');
                if (logisticsForm) {
                    logisticsForm.addEventListener('submit', async (evt) => {
                        evt.preventDefault();
                        const company = document.getElementById('logisticsCompany').value;
                        const trackId = document.getElementById('logisticsTrackId').value;
                        try {
                            const lRes = await api.patch(`/orders/${o.id}/logistics`, { trackId, company });
                            if (lRes.success) {
                                alert('Logistics updated and order status set to Processing!');
                                detailsModal.classList.add('hidden');
                                loadOrders();
                            } else {
                                alert(lRes.message || 'Failed to update logistics');
                            }
                        } catch (err) {
                            alert('Error: ' + err.message);
                        }
                    });
                }

                const completeBtn = document.getElementById('completeOrderBtn');
                if (completeBtn) {
                    completeBtn.addEventListener('click', async () => {
                        try {
                            const cRes = await api.patch(`/orders/${o.id}/complete`, {});
                            if (cRes.success) {
                                alert('Order completed successfully!');
                                detailsModal.classList.add('hidden');
                                loadOrders();
                            } else {
                                alert(cRes.message || 'Failed to complete order');
                            }
                        } catch (err) {
                            alert('Error: ' + err.message);
                        }
                    });
                }

                const openRefundBtn = document.getElementById('openRefundConvBtn');
                if (openRefundBtn) {
                    openRefundBtn.addEventListener('click', async () => {
                        try {
                            const rRes = await api.post(`/orders/${o.id}/refund`, { message: 'Refund conversation initiated by Admin.' });
                            if (rRes.success) {
                                alert('Refund conversation opened!');
                                await loadChatMessages(o.id);
                            } else {
                                alert(rRes.message || 'Failed to open refund conversation');
                            }
                        } catch (err) {
                            alert('Error: ' + err.message);
                        }
                    });
                }

                const chatForm = document.getElementById('chatMessageForm');
                if (chatForm) {
                    let chatAttachedImageBase64 = null;
                    const chatImageInput = document.getElementById('chatImageInput');
                    const chatImagePreviewContainer = document.getElementById('chatImagePreviewContainer');
                    const chatImagePreview = document.getElementById('chatImagePreview');
                    const chatRemoveImageBtn = document.getElementById('chatRemoveImageBtn');

                    if (chatImageInput) {
                        chatImageInput.addEventListener('change', async (e) => {
                            if (e.target.files && e.target.files[0]) {
                                try {
                                    chatAttachedImageBase64 = await fileToBase64(e.target.files[0]);
                                    chatImagePreview.src = chatAttachedImageBase64;
                                    chatImagePreviewContainer.style.display = 'flex';
                                } catch (err) {
                                    alert("Failed to read image file.");
                                }
                            }
                        });
                    }

                    if (chatRemoveImageBtn) {
                        chatRemoveImageBtn.addEventListener('click', () => {
                            chatAttachedImageBase64 = null;
                            chatImageInput.value = '';
                            chatImagePreview.src = '';
                            chatImagePreviewContainer.style.display = 'none';
                        });
                    }

                    chatForm.addEventListener('submit', async (evt) => {
                        evt.preventDefault();
                        const text = document.getElementById('chatInputText').value;
                        const payload = { message: text };
                        if (chatAttachedImageBase64) {
                            payload.screenshots = [chatAttachedImageBase64];
                        }
                        try {
                            const msgRes = await api.post(`/orders/${o.id}/refund/messages`, payload);
                            if (msgRes.success) {
                                document.getElementById('chatInputText').value = '';
                                if (chatRemoveImageBtn) chatRemoveImageBtn.click();
                                await loadChatMessages(o.id);
                            } else {
                                alert(msgRes.message || 'Failed to send message');
                            }
                        } catch (err) {
                            alert('Error sending message: ' + err.message);
                        }
                    });
                }

                const deleteOrderBtn = document.getElementById('permanentlyDeleteOrderBtn');
                if (deleteOrderBtn) {
                    deleteOrderBtn.addEventListener('click', async () => {
                        if (confirm(`Are you sure you want to delete order #${o.orderNumber || o.id}? This action cannot be undone.`)) {
                            try {
                                const delRes = await api.delete(`/orders/${o.id}`);
                                if (delRes.success) {
                                    alert('Order permanently deleted.');
                                    detailsModal.classList.add('hidden');
                                    loadOrders();
                                } else {
                                    alert(delRes.message || 'Failed to delete order');
                                }
                            } catch (err) {
                                alert('Error deleting order: ' + err.message);
                            }
                        }
                    });
                }
            }
        } catch (err) {
            alert('Failed to load order details: ' + err.message);
        }
    }

    async function loadChatMessages(orderId) {
        const chatContainer = document.getElementById('chatMessagesContainer');
        const chatForm = document.getElementById('chatMessageForm');
        const openRefundBtn = document.getElementById('openRefundConvBtn');
        if (!chatContainer) return;

        try {
            const res = await api.get(`/orders/${orderId}/refund/messages`);
            if (res.success && res.data) {
                const hasMessages = res.data.length > 0;

                if (chatForm) {
                    chatForm.style.display = hasMessages ? 'flex' : 'none';
                }
                if (openRefundBtn) {
                    openRefundBtn.style.display = hasMessages ? 'none' : 'inline-block';
                }

                if (!hasMessages) {
                    chatContainer.innerHTML = '<p style="font-size: 0.85rem; color: var(--text-secondary); text-align: center;">No refund conversation opened yet. Click "Open Refund Thread" to start.</p>';
                    return;
                }

                chatContainer.innerHTML = res.data.map(m => {
                    const hasImages = m.screenshots && m.screenshots.length > 0;
                    const imagesHtml = hasImages ? m.screenshots.map(src => `<img src="${src}" onclick="window.openImageViewer('${src}')" style="max-height: 100px; border-radius: 4px; border: 1px solid var(--border-color); margin-top: 0.5rem; cursor: zoom-in;" title="Click to view full size">`).join('') : '';
                    return `
                        <div style="background: var(--surface-color); padding: 0.5rem 0.75rem; border-radius: 6px; border: 1px solid var(--border-color);">
                            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 2px;">
                                <strong style="font-size: 0.85rem; color: var(--primary-color);">${m.senderName} (${m.senderRole})</strong>
                                <small style="font-size: 0.75rem; color: var(--text-secondary);">${new Date(m.createdAt).toLocaleTimeString()}</small>
                            </div>
                            <p style="font-size: 0.9rem; color: var(--text-primary); margin-bottom: 0;">${m.message}</p>
                            ${hasImages ? `<div style="display: flex; gap: 0.5rem; flex-wrap: wrap;">${imagesHtml}</div>` : ''}
                        </div>
                    `;
                }).join('');
                chatContainer.scrollTop = chatContainer.scrollHeight;
            }
        } catch (e) {
            chatContainer.innerHTML = '<p style="font-size: 0.85rem; color: var(--text-secondary); text-align: center;">No refund conversation opened yet.</p>';
            if (chatForm) chatForm.style.display = 'none';
            if (openRefundBtn) openRefundBtn.style.display = 'inline-block';
        }
    }

    if (closeDetailsModalBtn) closeDetailsModalBtn.addEventListener('click', () => {
        detailsModal.classList.add('hidden');
        loadOrders(); // Refresh to remove unread badges
    });
    if (closeDetailsBtn) closeDetailsBtn.addEventListener('click', () => {
        detailsModal.classList.add('hidden');
        loadOrders(); // Refresh to remove unread badges
    });

    await loadCatalog();
    loadOrders();
})();