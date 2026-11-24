(async function() {
    const tbody = document.getElementById('productsTableBody');
    const modal = document.getElementById('addProductModal');
    const addBtn = document.getElementById('addBtn');
    const closeBtn = document.getElementById('closeAddProductModal');
    const cancelBtn = document.getElementById('cancelAddProduct');
    const form = document.getElementById('addProductForm');
    const errorDiv = document.getElementById('productError');

    const detailsModal = document.getElementById('productDetailsModal');
    const closeDetailsModalBtn = document.getElementById('closeProductDetailsModal');
    const closeDetailsBtn = document.getElementById('closeDetailsBtn');
    const detailsBody = document.getElementById('productDetailsBody');
    const detailTitle = document.getElementById('detailProductTitle');

    const searchInput = document.getElementById('productSearchInput');
    const userFilterSelect = document.getElementById('productUserFilter');

    const userRole = localStorage.getItem('current_role') || 'Member';
    const currentUserId = parseInt(localStorage.getItem('current_user_id') || '0', 10);

    let allProducts = [];
    let currentFilter = 'all';
    let searchTerm = '';
    let selectedUserFilter = 'all';

    // Setup Creator Filter for Admins
    if (userRole === 'Admin' && userFilterSelect) {
        userFilterSelect.style.display = 'block';
        try {
            const uRes = await api.get('/users');
            if (uRes.success && uRes.data) {
                userFilterSelect.innerHTML = '<option value="all">All Creators</option>' +
                    uRes.data.map(u => `<option value="${u.name}">${u.name} (${u.username})</option>`).join('');
            }
        } catch (e) {
            console.error("Failed to load users for product filter", e);
        }

        userFilterSelect.addEventListener('change', (e) => {
            selectedUserFilter = e.target.value;
            renderProducts();
        });
    }

    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            searchTerm = e.target.value.toLowerCase().trim();
            renderProducts();
        });
    }

    // Filter tab clicks
    document.querySelectorAll('.product-tab').forEach(tab => {
        tab.addEventListener('click', (e) => {
            document.querySelectorAll('.product-tab').forEach(t => {
                t.style.fontWeight = 'normal';
                t.style.color = 'var(--text-secondary)';
                t.style.borderBottom = 'none';
            });
            e.target.style.fontWeight = 'bold';
            e.target.style.color = 'var(--primary-color)';
            e.target.style.borderBottom = '2px solid var(--primary-color)';
            e.target.style.paddingBottom = '0.25rem';

            currentFilter = e.target.dataset.filter;
            renderProducts();
        });
    });

    async function loadProducts() {
        try {
            const response = await api.get('/products');
            if (response.success && response.data) {
                allProducts = response.data;
                renderProducts();
            }
        } catch (error) {
            console.error("Failed to load products:", error);
            tbody.innerHTML = '<tr><td colspan="4" style="text-align:center; color: red;">Failed to load products</td></tr>';
        }
    }

    function renderProducts() {
        let filtered = allProducts;

        // Status tab filter
        if (currentFilter === 'needs_warehouse') {
            filtered = filtered.filter(p => !p.warehousePrice || p.warehousePrice === 0 || Number(p.warehousePrice) === 0);
        }

        // Creator filter
        if (selectedUserFilter !== 'all') {
            filtered = filtered.filter(p => p.creatorName === selectedUserFilter);
        }

        // Search term filter
        if (searchTerm) {
            filtered = filtered.filter(p =>
                (p.title && p.title.toLowerCase().includes(searchTerm)) ||
                (p.description && p.description.toLowerCase().includes(searchTerm)) ||
                (p.creatorName && p.creatorName.toLowerCase().includes(searchTerm))
            );
        }

        if (filtered.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" style="text-align:center; padding: 2rem; color: var(--text-secondary);">No products found matching your criteria</td></tr>';
            return;
        }

        // Smart Sorting: Products needing warehouse info come to the top
        filtered.sort((a, b) => {
            const needsWhA = !a.warehousePrice || a.warehousePrice === 0 || Number(a.warehousePrice) === 0;
            const needsWhB = !b.warehousePrice || b.warehousePrice === 0 || Number(b.warehousePrice) === 0;
            if (needsWhA && !needsWhB) return -1;
            if (!needsWhA && needsWhB) return 1;
            return b.id - a.id; // Recent first
        });

        tbody.innerHTML = filtered.map(p => {
            const needsWh = !p.warehousePrice || p.warehousePrice === 0 || Number(p.warehousePrice) === 0;
            return `
                <tr style="border-bottom: 1px solid var(--border-color); ${needsWh && (userRole === 'Admin' || userRole === 'Warehouse') ? 'background-color: rgba(239, 68, 68, 0.03);' : ''}">
                    <td style="padding: 1rem 0;">#${p.id}</td>
                    <td style="padding: 1rem 0; font-weight: bold;">
                        ${p.title}
                        <br><small style="color: var(--text-secondary);">Creator: ${p.creatorName || 'Admin'}</small>
                        ${needsWh && (userRole === 'Admin' || userRole === 'Warehouse') ? '<span style="margin-left: 8px; background: #fee2e2; color: #991b1b; padding: 0.15rem 0.4rem; border-radius: 4px; font-size: 0.75rem;">Needs Warehouse Info</span>' : ''}
                    </td>
                    <td style="padding: 1rem 0;">$${p.sourcePrice}</td>
                    <td style="padding: 1rem 0; display: flex; gap: 0.5rem;">
                        <button class="btn btn-outline view-product-btn" data-id="${p.id}" style="padding: 0.25rem 0.5rem; font-size: 0.875rem;">View</button>
                        ${userRole === 'Admin' ? `<button class="btn btn-outline delete-product-btn" data-id="${p.id}" style="padding: 0.25rem 0.5rem; font-size: 0.875rem; color: var(--danger-color); border-color: var(--danger-color);">Delete</button>` : ''}
                    </td>
                </tr>
            `;
        }).join('');

        // Attach View button listeners
        document.querySelectorAll('.view-product-btn').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                const productId = e.target.dataset.id;
                await openProductDetails(productId);
            });
        });

        // Attach Delete button listeners
        if (userRole === 'Admin') {
            document.querySelectorAll('.delete-product-btn').forEach(btn => {
                btn.addEventListener('click', async (e) => {
                    const productId = e.target.dataset.id;
                    if (confirm(`Are you sure you want to delete product #${productId}?`)) {
                        try {
                            const delRes = await api.delete(`/products/${productId}`);
                            if (delRes.success) {
                                loadProducts();
                            } else {
                                alert(delRes.message || 'Failed to delete product');
                            }
                        } catch (err) {
                            alert('Error deleting product: ' + err.message);
                        }
                    }
                });
            });
        }
    }

    async function openProductDetails(productId) {
        try {
            const res = await api.get(`/products/${productId}`);
            if (res.success && res.data) {
                const p = res.data;
                detailTitle.textContent = `#${p.id}: ${p.title}`;

                const isOwner = p.userId === currentUserId;
                const canManageShares = (userRole === 'Admin' || isOwner);

                // Fetch all system users for checkbox sharing
                let allUsers = [];
                try {
                    const userRes = await api.get('/users');
                    if (userRes.success && userRes.data) {
                        allUsers = userRes.data;
                    }
                } catch (e) {
                    console.error("Failed to load users for sharing", e);
                }

                let html = `
                    <div style="display: flex; gap: 1.5rem; align-items: flex-start;">
                        <img src="${p.productImageUrl}" alt="${p.title}" onclick="window.openImageViewer('${p.productImageUrl}')" style="width: 150px; height: 150px; border-radius: 8px; object-fit: cover; border: 1px solid var(--border-color); cursor: zoom-in;" title="Click to view full size">
                        <div style="flex: 1;">
                            <p style="color: var(--text-secondary); margin-bottom: 0.5rem;">${p.description || 'No description provided.'}</p>
                            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-top: 1rem;">
                                <div><strong>Creator:</strong> ${p.creatorName || 'Admin'}</div>
                                <div><strong>Source Price:</strong> $${p.sourcePrice}</div>
                                <div><strong>Reference Price:</strong> $${p.referencePrice}</div>
                                <div><strong>Warehouse Price:</strong> ${p.warehousePrice && p.warehousePrice > 0 ? '$' + p.warehousePrice : '<span style="color: var(--danger-color)">Pending Warehouse Review</span>'}</div>
                            </div>
                        </div>
                    </div>

                    <div style="display: flex; gap: 1rem; margin-top: 0.5rem;">
                        <a href="${p.sourceLink}" target="_blank" class="btn btn-outline" style="flex: 1; text-align: center; text-decoration: none;"><i class="fas fa-external-link-alt"></i> View Source Link</a>
                        <a href="${p.referenceLink}" target="_blank" class="btn btn-outline" style="flex: 1; text-align: center; text-decoration: none;"><i class="fas fa-external-link-alt"></i> View Reference Link</a>
                    </div>
                `;

                // Warehouse Inspection Image Preview if exists
                if (p.warehouseImageUrl) {
                    html += `
                        <div style="margin-top: 1rem;">
                            <strong>Warehouse Inspection / Alternative Image:</strong><br>
                            <img src="${p.warehouseImageUrl}" alt="Warehouse Inspection" onclick="window.openImageViewer('${p.warehouseImageUrl}')" style="max-width: 200px; max-height: 150px; border-radius: 6px; margin-top: 0.5rem; border: 1px solid var(--border-color); cursor: zoom-in;" title="Click to view full size">
                        </div>
                    `;
                }

                // Shared Members Checkboxes Section
                html += `
                    <div style="margin-top: 1rem; background: var(--background-color); padding: 1rem; border-radius: 8px; border: 1px solid var(--border-color);">
                        <h4 style="margin-bottom: 0.5rem; color: var(--primary-color);"><i class="fas fa-share-alt"></i> Share With Members</h4>
                        <p style="font-size: 0.9rem; color: var(--text-secondary); margin-bottom: 0.75rem;">
                            Currently shared with: <strong>${p.sharedUsernames && p.sharedUsernames.length > 0 ? p.sharedUsernames.join(', ') : 'None'}</strong>
                        </p>
                        ${canManageShares ? `
                            <form id="productSharesForm" style="display: flex; flex-direction: column; gap: 0.75rem; margin-top: 0.5rem;">
                                <div style="max-height: 150px; overflow-y: auto; background: var(--surface-color); padding: 0.5rem 0.75rem; border: 1px solid var(--border-color); border-radius: 6px;">
                                    ${allUsers.length > 0 ? allUsers.map(u => {
                                        const isOwner = u.id === p.userId;
                                        const isAdmin = u.role === 'Admin';
                                        const isProtected = isOwner || isAdmin;
                                        const isChecked = isProtected || (p.sharedUsernames && p.sharedUsernames.includes(u.username));
                                        return `
                                            <label style="display: flex; align-items: center; gap: 8px; font-size: 0.875rem; cursor: ${isProtected ? 'not-allowed' : 'pointer'}; padding: 4px 0; border-bottom: 1px solid #f3f4f6; opacity: ${isProtected ? '0.7' : '1'};">
                                                <input type="checkbox" class="share-user-checkbox" value="${u.username}" ${isChecked ? 'checked' : ''} ${isProtected ? 'disabled' : ''}>
                                                <span><strong>${u.username}</strong> (${u.name} - <span style="color: var(--primary-color);">${u.role}</span>)
                                                    ${isOwner ? '<span style="font-size:0.75rem; color:#6b7280; font-style:italic;">(Self / Owner)</span>' : ''}
                                                    ${isAdmin && !isOwner ? '<span style="font-size:0.75rem; color:#6b7280; font-style:italic;">(Admin)</span>' : ''}
                                                </span>
                                            </label>
                                        `;
                                    }).join('') : '<p style="font-size: 0.85rem; color: var(--text-secondary);">No other users available.</p>'}
                                </div>
                                <button type="submit" class="btn btn-primary" style="align-self: flex-end; padding: 0.4rem 1.25rem;">Update Shares</button>
                            </form>
                        ` : '<p style="font-size: 0.8rem; font-style: italic; color: var(--text-secondary);">Only the product creator or an Admin can modify shares.</p>'}
                    </div>
                `;

                // Warehouse Notes & Evaluation Form
                if (p.warehouseNote || userRole === 'Admin' || userRole === 'Warehouse') {
                    html += `
                        <div style="background: var(--background-color); padding: 1rem; border-radius: 8px; border: 1px solid var(--border-color); margin-top: 1rem;">
                            <h4 style="margin-bottom: 0.75rem; color: var(--primary-color);"><i class="fas fa-warehouse"></i> Warehouse Evaluation & Alternative Image</h4>
                            <p style="font-size: 0.9rem; margin-bottom: 0.75rem;"><strong>Note:</strong> ${p.warehouseNote || 'No notes from warehouse yet.'}</p>
                            ${userRole === 'Admin' || userRole === 'Warehouse' ? `
                                <form id="warehouseEvalForm" style="display: flex; flex-direction: column; gap: 1rem;">
                                    <div style="display: flex; gap: 1rem;">
                                        <div style="flex: 1;">
                                            <label style="font-size: 0.85rem; color: var(--text-secondary);">Warehouse Price ($)</label>
                                            <input type="number" step="0.01" id="evalWarehousePrice" class="form-control" value="${p.warehousePrice || ''}" required>
                                        </div>
                                    </div>
                                    <div>
                                        <label style="font-size: 0.85rem; color: var(--text-secondary);">Warehouse Note</label>
                                        <textarea id="evalWarehouseNote" class="form-control" rows="2">${p.warehouseNote || ''}</textarea>
                                    </div>
                                    <div>
                                        <label style="font-size: 0.85rem; color: var(--text-secondary);">Alternative / Inspection Image (Drag & Drop or Browse)</label>
                                        <div id="whDropzone" class="dropzone" style="padding: 0.75rem;">
                                            <p style="font-size: 0.85rem; color: var(--text-secondary);">Drop alternative image here, or <span style="color: var(--primary-color);">browse</span></p>
                                            <input type="file" id="whImgFile" accept="image/*" style="display: none;">
                                        </div>
                                        <div id="whImgPreviewContainer" class="dropzone-preview-container ${p.warehouseImageUrl ? '' : 'hidden'}">
                                            <img id="whImgPreview" class="dropzone-preview" src="${p.warehouseImageUrl || ''}" alt="Preview">
                                            <button type="button" id="removeWhImg" class="btn btn-outline" style="padding: 0.2rem 0.5rem; font-size: 0.75rem; color: var(--danger-color);">Remove</button>
                                        </div>
                                        <input type="hidden" id="evalWarehouseImageUrl" value="${p.warehouseImageUrl || ''}">
                                    </div>
                                    <button type="submit" class="btn btn-primary" style="align-self: flex-end; padding: 0.4rem 1.25rem;">Save Warehouse Evaluation</button>
                                </form>
                            ` : ''}
                        </div>
                    `;
                }

                detailsBody.innerHTML = html;
                detailsModal.classList.remove('hidden');

                // Setup Product Shares Form listener if present
                const sharesForm = document.getElementById('productSharesForm');
                if (sharesForm) {
                    sharesForm.addEventListener('submit', async (evt) => {
                        evt.preventDefault();
                        const checkedCheckboxes = document.querySelectorAll('.share-user-checkbox:checked');
                        let shareWithUsernames = Array.from(checkedCheckboxes).map(cb => cb.value);

                        allUsers.forEach(u => {
                            if ((u.id === p.userId || u.role === 'Admin') && !shareWithUsernames.includes(u.username)) {
                                shareWithUsernames.push(u.username);
                            }
                        });

                        try {
                            const shareRes = await api.patch(`/products/${productId}/shares`, {
                                shareWithUsernames: shareWithUsernames
                            });
                            if (shareRes.success) {
                                alert('Product shares updated successfully!');
                                detailsModal.classList.add('hidden');
                                loadProducts();
                            } else {
                                alert(shareRes.message || 'Failed to update shares');
                            }
                        } catch (err) {
                            alert('Error updating shares: ' + err.message);
                        }
                    });
                }

                // Setup Warehouse Dropzone if present
                const whDropzone = document.getElementById('whDropzone');
                const whFileInput = document.getElementById('whImgFile');
                const whPreviewContainer = document.getElementById('whImgPreviewContainer');
                const whPreviewImg = document.getElementById('whImgPreview');
                const whRemoveBtn = document.getElementById('removeWhImg');
                const whImageHidden = document.getElementById('evalWarehouseImageUrl');

                if (whDropzone && whFileInput) {
                    whDropzone.addEventListener('click', () => whFileInput.click());
                    whDropzone.addEventListener('dragover', (e) => { e.preventDefault(); whDropzone.classList.add('dragover'); });
                    whDropzone.addEventListener('dragleave', () => { whDropzone.classList.remove('dragover'); });
                    whDropzone.addEventListener('drop', async (e) => {
                        e.preventDefault();
                        whDropzone.classList.remove('dragover');
                        if (e.dataTransfer.files && e.dataTransfer.files[0]) {
                            await handleWhImageFile(e.dataTransfer.files[0]);
                        }
                    });
                    whFileInput.addEventListener('change', async (e) => {
                        if (e.target.files && e.target.files[0]) {
                            await handleWhImageFile(e.target.files[0]);
                        }
                    });
                    whRemoveBtn.addEventListener('click', () => {
                        whImageHidden.value = '';
                        whFileInput.value = '';
                        whPreviewImg.src = '';
                        whPreviewContainer.classList.add('hidden');
                        whDropzone.style.display = 'block';
                    });
                }

                async function handleWhImageFile(file) {
                    if (!file.type.startsWith('image/')) {
                        alert('Please select a valid image file.');
                        return;
                    }
                    try {
                        const base64String = await fileToBase64(file);
                        whImageHidden.value = base64String;
                        whPreviewImg.src = base64String;
                        whDropzone.style.display = 'none';
                        whPreviewContainer.classList.remove('hidden');
                    } catch (err) {
                        console.error('Failed to convert warehouse image to Base64', err);
                    }
                }

                // Attach Warehouse Evaluation Form listener if present
                const evalForm = document.getElementById('warehouseEvalForm');
                if (evalForm) {
                    evalForm.addEventListener('submit', async (evt) => {
                        evt.preventDefault();
                        const whPrice = parseFloat(document.getElementById('evalWarehousePrice').value);
                        const whNote = document.getElementById('evalWarehouseNote').value;
                        const whImageUrl = document.getElementById('evalWarehouseImageUrl').value;

                        try {
                            const whRes = await api.patch(`/products/${productId}/warehouse`, {
                                warehousePrice: whPrice,
                                warehouseNote: whNote,
                                warehouseImageUrl: whImageUrl
                            });
                            if (whRes.success) {
                                alert('Warehouse evaluation and alternative image saved successfully!');
                                detailsModal.classList.add('hidden');
                                loadProducts();
                            } else {
                                alert(whRes.message || 'Failed to update warehouse evaluation');
                            }
                        } catch (err) {
                            alert('Error: ' + err.message);
                        }
                    });
                }
            }
        } catch (err) {
            alert('Failed to load product details: ' + err.message);
        }
    }

    // Modal behavior for Add Product
    function openModal() { modal.classList.remove('hidden'); errorDiv.style.display = 'none'; }
    function closeModal() {
        modal.classList.add('hidden');
        form.reset();
        const previewContainer = document.getElementById('prodImgPreviewContainer');
        const dropzone = document.getElementById('productDropzone');
        const imgUrlHidden = document.getElementById('prodImgUrl');
        if (previewContainer) previewContainer.classList.add('hidden');
        if (dropzone) dropzone.style.display = 'block';
        if (imgUrlHidden) imgUrlHidden.value = '';
    }

    // Dropzone setup for Add Product
    const dropzone = document.getElementById('productDropzone');
    const fileInput = document.getElementById('prodImgFile');
    const previewContainer = document.getElementById('prodImgPreviewContainer');
    const previewImg = document.getElementById('prodImgPreview');
    const fileNameSpan = document.getElementById('prodImgFileName');
    const removeImgBtn = document.getElementById('removeProdImg');
    const imgUrlHidden = document.getElementById('prodImgUrl');

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
            imgUrlHidden.value = '';
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
            imgUrlHidden.value = base64String;
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

    if (addBtn) addBtn.addEventListener('click', openModal);
    if (closeBtn) closeBtn.addEventListener('click', closeModal);
    if (cancelBtn) cancelBtn.addEventListener('click', closeModal);

    // Modal behavior for Details
    if (closeDetailsModalBtn) closeDetailsModalBtn.addEventListener('click', () => detailsModal.classList.add('hidden'));
    if (closeDetailsBtn) closeDetailsBtn.addEventListener('click', () => detailsModal.classList.add('hidden'));

    if (form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const payload = {
                title: document.getElementById('prodTitle').value,
                description: document.getElementById('prodDesc').value,
                productImageUrl: document.getElementById('prodImgUrl').value,
                sourceLink: document.getElementById('prodSourceLink').value,
                sourcePrice: parseFloat(document.getElementById('prodSourcePrice').value),
                referenceLink: document.getElementById('prodRefLink').value,
                referencePrice: parseFloat(document.getElementById('prodRefPrice').value)
            };

            try {
                const response = await api.post('/products', payload);
                if (response.success) {
                    closeModal();
                    loadProducts();
                } else {
                    throw new Error(response.message || 'Failed to create product');
                }
            } catch (err) {
                errorDiv.textContent = err.message;
                errorDiv.style.display = 'block';
            }
        });
    }

    // Initial load
    loadProducts();
})();