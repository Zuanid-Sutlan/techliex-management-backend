(async function() {
    const container = document.getElementById('ordersContainer');
    const modal = document.getElementById('addOrderModal');
    const addBtn = document.getElementById('addOrderBtn');
    const closeBtn = document.getElementById('closeAddOrderModal');
    const cancelBtn = document.getElementById('cancelAddOrder');
    const form = document.getElementById('addOrderForm');
    const errorDiv = document.getElementById('orderError');

    async function loadOrders() {
        try {
            const response = await api.get('/orders');

            if (response.success && response.data) {
                if (response.data.length === 0) {
                    container.innerHTML = `<div style="text-align: center; padding: 2rem; color: var(--text-secondary);">
                        <i class="fas fa-inbox" style="font-size: 3rem; margin-bottom: 1rem; opacity: 0.5;"></i>
                        <p>No orders found.</p>
                    </div>`;
                    return;
                }

                let html = '<table style="width: 100%; border-collapse: collapse;"><thead><tr style="border-bottom: 2px solid var(--border-color); text-align: left;"><th style="padding: 1rem 0;">Order #</th><th style="padding: 1rem 0;">Product</th><th style="padding: 1rem 0;">Status</th><th style="padding: 1rem 0;">Actions</th></tr></thead><tbody>';

                html += response.data.map(o => `
                    <tr style="border-bottom: 1px solid var(--border-color);">
                        <td style="padding: 1rem 0; font-weight: bold;">${o.orderNumber}</td>
                        <td style="padding: 1rem 0;">${o.productName}</td>
                        <td style="padding: 1rem 0;">
                            <span style="background: #e0e7ff; color: #4338ca; padding: 0.25rem 0.5rem; border-radius: 4px; font-size: 0.875rem;">${o.status}</span>
                        </td>
                        <td style="padding: 1rem 0;"><button class="btn btn-outline" style="padding: 0.25rem 0.5rem; font-size: 0.875rem;">Manage</button></td>
                    </tr>
                `).join('');

                html += '</tbody></table>';
                container.innerHTML = html;
            }
        } catch (error) {
            console.error("Failed to load orders:", error);
            container.innerHTML = '<p style="color: red;">Failed to load orders</p>';
        }
    }

    // Modal behavior
    function openModal() { modal.classList.remove('hidden'); errorDiv.style.display = 'none'; }
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

    // Dropzone setup for orders
    const dropzone = document.getElementById('orderDropzone');
    const fileInput = document.getElementById('orderImgFile');
    const previewContainer = document.getElementById('orderImgPreviewContainer');
    const previewImg = document.getElementById('orderImgPreview');
    const fileNameSpan = document.getElementById('orderImgFileName');
    const removeImgBtn = document.getElementById('removeOrderImg');
    const screenshotHidden = document.getElementById('orderPaymentScreenshot');

    if (dropzone && fileInput) {
        dropzone.addEventListener('click', () => fileInput.click());

        dropzone.addEventListener('dragover', (e) => {
            e.preventDefault();
            dropzone.classList.add('dragover');
        });

        dropzone.addEventListener('dragleave', () => {
            dropzone.classList.remove('dragover');
        });

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
            console.error('Failed to convert image to Base64', err);
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
            const payload = {
                orderNumber: document.getElementById('orderNumber').value,
                orderDate: document.getElementById('orderDate').value,
                productName: document.getElementById('orderProductName').value,
                address: document.getElementById('orderAddress').value,
                quantity: parseInt(document.getElementById('orderQuantity').value),
                paymentScreenshot: document.getElementById('orderPaymentScreenshot').value || null,
                listingPrice: 0.0,
                status: 'Active'
            };

            try {
                const response = await api.post('/orders', payload);
                if (response.success) {
                    closeModal();
                    loadOrders();
                } else {
                    throw new Error(response.message || 'Failed to create order');
                }
            } catch (err) {
                errorDiv.textContent = err.message;
                errorDiv.style.display = 'block';
            }
        });
    }

    // Initial load
    loadOrders();
})();