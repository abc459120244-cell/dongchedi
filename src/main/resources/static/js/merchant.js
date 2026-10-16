/* ==========================================================================
   商家工作台脚本：车源管理（增删改）+ 订单处理 + SSE 实时消息
   对应后端接口：
   GET/POST/PUT/DELETE /api/merchant/cars     车源管理
   GET  /api/merchant/orders                  收到的订单
   POST /api/merchant/orders/{id}/status      处理订单
   GET  /api/merchant/summary                 经营概览
   GET  /api/notifications/stream             SSE 实时消息
   ========================================================================== */
(function () {
    'use strict';

    var state = {
        user: null,
        cars: [],
        orders: [],
        editingId: null,
        handlingOrderId: null,
        orderFilter: '',
        stream: null
    };

    var dom = {};

    function $(selector) {
        return document.querySelector(selector);
    }

    function escapeHtml(value) {
        if (value === null || value === undefined) {
            return '';
        }
        return String(value).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    function num(value) {
        return value === null || value === undefined ? 0 : Number(value);
    }

    function fetchJson(url, options) {
        return fetch(url, options).then(function (response) {
            if (!response.ok) {
                return response.json().catch(function () {
                    return {};
                }).then(function (body) {
                    var message = body && body.message ? body.message : ('请求失败：HTTP ' + response.status);
                    if (body && body.details && body.details.length) {
                        message += '（' + body.details.join('；') + '）';
                    }
                    return Promise.reject(new Error(message));
                });
            }
            return response.status === 204 ? null : response.json();
        });
    }

    var toastTimer = null;

    function toast(message, type) {
        dom.toast.textContent = message;
        dom.toast.className = 'toast' + (type === 'success' ? ' toast--ok' : '');
        dom.toast.hidden = false;
        if (toastTimer) {
            clearTimeout(toastTimer);
        }
        toastTimer = setTimeout(function () {
            dom.toast.hidden = true;
        }, type === 'success' ? 4200 : 2600);
    }

    function postJson(url, body) {
        return fetchJson(url, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(body || {})
        });
    }

    /* ------------------------------- 初始化 ------------------------------- */

    function init() {
        dom.loginSection = $('#loginSection');
        dom.workbench = $('#workbench');
        dom.userArea = $('#userArea');
        dom.loginForm = $('#loginForm');
        dom.loginUsername = $('#loginUsername');
        dom.loginPassword = $('#loginPassword');
        dom.loginError = $('#loginError');
        dom.loginBtn = $('#loginBtn');
        dom.statCards = $('#statCards');
        dom.carTableBody = $('#carTableBody');
        dom.orderTableBody = $('#orderTableBody');
        dom.orderFilter = $('#orderFilter');
        dom.carModal = $('#carModal');
        dom.carForm = $('#carForm');
        dom.carModalTitle = $('#carModalTitle');
        dom.carError = $('#carError');
        dom.carSubmitBtn = $('#carSubmitBtn');
        dom.orderModal = $('#orderModal');
        dom.orderModalInfo = $('#orderModalInfo');
        dom.orderStatusSelect = $('#orderStatusSelect');
        dom.orderReply = $('#orderReply');
        dom.orderSubmitBtn = $('#orderSubmitBtn');
        dom.messageModal = $('#messageModal');
        dom.messageContent = $('#messageContent');
        dom.toast = $('#toast');

        bindEvents();
        loadSession();
    }

    function bindEvents() {
        dom.loginForm.addEventListener('submit', function (event) {
            event.preventDefault();
            dom.loginError.hidden = true;
            dom.loginBtn.disabled = true;
            postJson('/api/auth/login', {
                username: dom.loginUsername.value.trim(),
                password: dom.loginPassword.value
            }).then(function (user) {
                if (user.role !== 'MERCHANT' && user.role !== 'ADMIN') {
                    throw new Error('该账号不是商家账号，请使用 dealer_byd / dealer_ev / dealer_intl 登录');
                }
                state.user = user;
                dom.loginPassword.value = '';
                enterWorkbench();
            }).catch(function (error) {
                dom.loginError.textContent = error.message;
                dom.loginError.hidden = false;
            }).finally(function () {
                dom.loginBtn.disabled = false;
            });
        });

        dom.userArea.addEventListener('click', function (event) {
            var action = event.target.closest('[data-action]');
            if (!action) {
                return;
            }
            if (action.dataset.action === 'messages') {
                openMessages();
            } else if (action.dataset.action === 'logout') {
                postJson('/api/auth/logout', {}).then(function () {
                    state.user = null;
                    if (state.stream) {
                        state.stream.close();
                        state.stream = null;
                    }
                    dom.workbench.hidden = true;
                    dom.loginSection.hidden = false;
                    renderUserArea();
                    toast('已退出登录');
                });
            }
        });

        $('#addCarBtn').addEventListener('click', function () {
            openCarForm(null);
        });

        dom.carTableBody.addEventListener('click', function (event) {
            var button = event.target.closest('button[data-car-action]');
            if (!button) {
                return;
            }
            var id = Number(button.dataset.id);
            var action = button.dataset.carAction;
            if (action === 'edit') {
                openCarForm(state.cars.find(function (car) {
                    return car.id === id;
                }));
            } else if (action === 'toggle') {
                toggleStatus(id, button.dataset.next);
            } else if (action === 'delete') {
                deleteCar(id);
            }
        });

        dom.carModal.addEventListener('click', function (event) {
            if (event.target.closest('[data-car-close]')) {
                closeCarForm();
            }
        });
        dom.carForm.addEventListener('submit', submitCarForm);

        dom.orderFilter.addEventListener('click', function (event) {
            var button = event.target.closest('button[data-status]');
            if (!button) {
                return;
            }
            Array.prototype.forEach.call(dom.orderFilter.querySelectorAll('button'), function (item) {
                item.classList.toggle('is-active', item === button);
            });
            state.orderFilter = button.dataset.status;
            loadOrders();
        });

        dom.orderTableBody.addEventListener('click', function (event) {
            var button = event.target.closest('button[data-order-action="handle"]');
            if (!button) {
                return;
            }
            openOrderModal(Number(button.dataset.id));
        });

        dom.orderModal.addEventListener('click', function (event) {
            if (event.target.closest('[data-order-close]')) {
                dom.orderModal.hidden = true;
            }
        });
        dom.orderSubmitBtn.addEventListener('click', submitOrderHandle);

        dom.messageModal.addEventListener('click', function (event) {
            if (event.target.closest('[data-message-close]')) {
                dom.messageModal.hidden = true;
            }
        });

        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape') {
                closeCarForm();
                dom.orderModal.hidden = true;
                dom.messageModal.hidden = true;
            }
        });
    }

    /* ------------------------------ 登录与会话 ------------------------------ */

    function loadSession() {
        fetchJson('/api/auth/me').then(function (user) {
            if (user.role !== 'MERCHANT' && user.role !== 'ADMIN') {
                showLogin('当前账号「' + (user.displayName || user.username)
                    + '」不是商家账号，请使用商家账号登录（演示账号见下方）。');
                return;
            }
            state.user = user;
            enterWorkbench();
        }).catch(function () {
            showLogin();
        });
    }

    function showLogin(message) {
        dom.workbench.hidden = true;
        dom.loginSection.hidden = false;
        state.user = null;
        renderUserArea();
        if (message) {
            dom.loginError.textContent = message;
            dom.loginError.hidden = false;
        }
    }

    function enterWorkbench() {
        dom.loginSection.hidden = true;
        dom.workbench.hidden = false;
        renderUserArea();
        loadFilterOptions();
        loadSummary();
        loadCars();
        loadOrders();
        connectStream();
    }

    function renderUserArea() {
        if (!state.user) {
            dom.userArea.innerHTML = '';
            return;
        }
        dom.userArea.innerHTML = '<span class="user-chip">'
            + '<span class="user-chip__avatar">商</span>'
            + '<span class="user-chip__name">' + escapeHtml(state.user.displayName) + '</span>'
            + (state.user.role === 'ADMIN' ? '<em class="user-chip__role">管理员</em>' : '')
            + '</span>'
            + '<button type="button" class="icon-btn" data-action="messages">消息 <em id="unreadBadge">'
            + (state.unread || 0) + '</em></button>'
            + '<button type="button" class="icon-btn" data-action="logout">退出</button>';
    }

    /* ------------------------------- 数据加载 ------------------------------- */

    function loadFilterOptions() {
        fetchJson('/api/cars/filters').then(function (filters) {
            fillSelect('#carLevel', filters.levels);
            fillSelect('#carBodyType', filters.bodyTypes);
            fillSelect('#carEnergyType', filters.energyTypes);
            $('#brandList').innerHTML = filters.brands.map(function (item) {
                return '<option value="' + escapeHtml(item.label) + '"></option>';
            }).join('');
        }).catch(function () {
            /* 下拉选项加载失败不影响主体功能 */
        });
    }

    function fillSelect(selector, options) {
        var select = $(selector);
        if (!select || !options) {
            return;
        }
        select.innerHTML = options.map(function (item) {
            return '<option value="' + escapeHtml(item.code) + '">' + escapeHtml(item.label) + '</option>';
        }).join('');
    }

    function loadSummary() {
        return fetchJson('/api/merchant/summary').then(function (summary) {
            state.unread = summary.unreadNotifications;
            dom.statCards.innerHTML = [
                {value: summary.onSaleCars, label: '在售车源', unit: '款'},
                {value: summary.offShelfCars, label: '已下架', unit: '款'},
                {value: summary.pendingOrders, label: '待处理订单', unit: '单', highlight: summary.pendingOrders > 0},
                {value: summary.unreadNotifications, label: '未读消息', unit: '条', highlight: summary.unreadNotifications > 0},
                {value: num(summary.totalMonthlySales).toLocaleString('zh-CN'), label: '车源月销量合计', unit: '辆'}
            ].map(function (card) {
                return '<div class="stat-card' + (card.highlight ? ' stat-card--alert' : '') + '">'
                    + '<div class="stat-card__value">' + escapeHtml(card.value)
                    + '<small>' + card.unit + '</small></div>'
                    + '<div class="stat-card__label">' + escapeHtml(card.label) + '</div></div>';
            }).join('');
            renderUserArea();
            return summary;
        });
    }

    function loadCars() {
        return fetchJson('/api/merchant/cars').then(function (cars) {
            state.cars = cars;
            if (!cars.length) {
                dom.carTableBody.innerHTML = '<tr><td colspan="8" class="placeholder">还没有车源，点右上角「新增车源」试试</td></tr>';
                return;
            }
            dom.carTableBody.innerHTML = cars.map(function (car) {
                var isOnSale = car.status === 'ON_SALE';
                return '<tr>'
                    + '<td>' + car.id + '</td>'
                    + '<td><div class="rank-car">'
                    + '<span class="thumb thumb--sm" style="background:linear-gradient(135deg,#ff4d1f,#ff8a3d)">'
                    + '<img src="/img/cars/photos/' + car.id + '.jpg" alt="" loading="lazy"'
                    + ' onerror="this.onerror=null;this.remove()"></span>'
                    + '<span>' + escapeHtml(car.name) + '</span></div></td>'
                    + '<td>' + escapeHtml(car.brand) + '</td>'
                    + '<td>' + escapeHtml(car.energyTypeLabel) + '</td>'
                    + '<td>' + num(car.price).toFixed(2) + ' 万</td>'
                    + '<td>' + num(car.monthlySales).toLocaleString('zh-CN') + '</td>'
                    + '<td><span class="tag ' + (isOnSale ? 'tag--ok' : 'tag--off') + '">'
                    + escapeHtml(car.statusLabel) + '</span></td>'
                    + '<td class="table-actions">'
                    + '<button type="button" class="icon-btn" data-car-action="edit" data-id="' + car.id + '">编辑</button>'
                    + '<button type="button" class="icon-btn" data-car-action="toggle" data-id="' + car.id
                    + '" data-next="' + (isOnSale ? 'OFF_SHELF' : 'ON_SALE') + '">' + (isOnSale ? '下架' : '上架') + '</button>'
                    + '<button type="button" class="icon-btn icon-btn--danger" data-car-action="delete" data-id="'
                    + car.id + '">删除</button>'
                    + '</td></tr>';
            }).join('');
        }).catch(function (error) {
            toast(error.message);
        });
    }

    function loadOrders() {
        var url = '/api/merchant/orders' + (state.orderFilter ? '?status=' + state.orderFilter : '');
        return fetchJson(url).then(function (orders) {
            state.orders = orders;
            if (!orders.length) {
                dom.orderTableBody.innerHTML = '<tr><td colspan="9" class="placeholder">暂无订单</td></tr>';
                return;
            }
            dom.orderTableBody.innerHTML = orders.map(function (order) {
                var pending = order.status === 'PENDING';
                return '<tr>'
                    + '<td>#' + order.id + '</td>'
                    + '<td>' + escapeHtml(order.carName) + '</td>'
                    + '<td>' + escapeHtml(order.buyerName) + '</td>'
                    + '<td>' + escapeHtml(order.contactName) + '<br><small>' + escapeHtml(order.contactPhone) + '</small></td>'
                    + '<td>' + escapeHtml(order.typeLabel) + '</td>'
                    + '<td>' + escapeHtml(order.remark || '-') + '</td>'
                    + '<td><span class="tag ' + statusClass(order.status) + '">' + escapeHtml(order.statusLabel) + '</span>'
                    + (order.dealerReply ? '<br><small>回复：' + escapeHtml(order.dealerReply) + '</small>' : '') + '</td>'
                    + '<td><small>' + escapeHtml((order.createdAt || '').replace('T', ' ').substring(0, 16)) + '</small></td>'
                    + '<td class="table-actions">'
                    + (pending
                        ? '<button type="button" class="icon-btn" data-order-action="handle" data-id="' + order.id + '">处理</button>'
                        : '<span class="placeholder">已处理</span>')
                    + '</td></tr>';
            }).join('');
        }).catch(function (error) {
            toast(error.message);
        });
    }

    function statusClass(status) {
        if (status === 'PENDING') {
            return 'tag--warn';
        }
        if (status === 'CONFIRMED') {
            return 'tag--ok';
        }
        if (status === 'REJECTED') {
            return 'tag--off';
        }
        return 'tag--done';
    }

    /* ------------------------------ 车源增删改 ------------------------------ */

    function openCarForm(car) {
        state.editingId = car ? car.id : null;
        dom.carModalTitle.textContent = car ? ('编辑车源 #' + car.id) : '新增车源';
        $('#carName').value = car ? car.name : '';
        $('#carBrand').value = car ? car.brand : '';
        $('#carLevel').value = car ? car.level : 'COMPACT';
        $('#carBodyType').value = car ? car.bodyType : 'SUV';
        $('#carEnergyType').value = car ? (car.energyTypeLabel ? codeOfEnergy(car.energyTypeLabel) : 'GASOLINE') : 'GASOLINE';
        $('#carPrice').value = car ? car.price : '';
        $('#carSeats').value = car ? car.seats : 5;
        $('#carPower').value = car ? car.maxPowerKw : 150;
        $('#carAcceleration').value = car ? car.zeroToHundredSeconds : 8;
        $('#carSales').value = car ? car.monthlySales : 0;
        $('#carScore').value = car ? car.userScore : 4.2;
        $('#carStatus').value = car ? car.status : 'ON_SALE';
        $('#carTags').value = car && car.tags ? car.tags.join(',') : '';
        $('#carHighlight').value = car ? (car.highlight || '') : '';
        dom.carError.hidden = true;
        dom.carModal.hidden = false;
        $('#carName').focus();
    }

    function codeOfEnergy(label) {
        var map = {
            '汽油': 'GASOLINE', '柴油': 'DIESEL', '油电混合': 'HEV',
            '插电式混合动力': 'PHEV', '增程式电动': 'EREV', '纯电动': 'BEV'
        };
        return map[label] || 'GASOLINE';
    }

    function closeCarForm() {
        dom.carModal.hidden = true;
        state.editingId = null;
    }

    function submitCarForm(event) {
        event.preventDefault();
        var payload = {
            name: $('#carName').value.trim(),
            brand: $('#carBrand').value.trim(),
            level: $('#carLevel').value,
            bodyType: $('#carBodyType').value,
            energyType: $('#carEnergyType').value,
            price: Number($('#carPrice').value),
            seats: Number($('#carSeats').value),
            maxPowerKw: Number($('#carPower').value),
            zeroToHundredSeconds: Number($('#carAcceleration').value),
            monthlySales: Number($('#carSales').value),
            userScore: Number($('#carScore').value),
            status: $('#carStatus').value,
            tags: $('#carTags').value.split(',').map(function (item) {
                return item.trim();
            }).filter(function (item) {
                return item.length > 0;
            }),
            scenes: [],
            highlight: $('#carHighlight').value.trim()
        };
        if (!payload.name || !payload.brand || !payload.price) {
            dom.carError.textContent = '车型名称、品牌、指导价为必填项';
            dom.carError.hidden = false;
            return;
        }

        dom.carSubmitBtn.disabled = true;
        var request = state.editingId
            ? fetchJson('/api/merchant/cars/' + state.editingId, {
                method: 'PUT',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(payload)
            })
            : postJson('/api/merchant/cars', payload);

        request.then(function () {
            closeCarForm();
            toast(state.editingId ? '车源已更新，数据库已同步' : '车源已新增，买家端即可看到', 'success');
            loadCars();
            loadSummary();
        }).catch(function (error) {
            dom.carError.textContent = error.message;
            dom.carError.hidden = false;
        }).finally(function () {
            dom.carSubmitBtn.disabled = false;
        });
    }

    function toggleStatus(id, nextStatus) {
        fetchJson('/api/merchant/cars/' + id + '/status?status=' + nextStatus, {method: 'POST'})
            .then(function () {
                toast(nextStatus === 'ON_SALE' ? '已上架，买家端可见' : '已下架，买家端不再展示', 'success');
                loadCars();
                loadSummary();
            }).catch(function (error) {
                toast(error.message);
            });
    }

    function deleteCar(id) {
        var car = state.cars.find(function (item) {
            return item.id === id;
        });
        if (!window.confirm('确定删除车源「' + (car ? car.name : id) + '」吗？删除后不可恢复。')) {
            return;
        }
        fetchJson('/api/merchant/cars/' + id, {method: 'DELETE'}).then(function () {
            toast('车源已删除，数据库已同步', 'success');
            loadCars();
            loadSummary();
        }).catch(function (error) {
            toast(error.message);
        });
    }

    /* ------------------------------- 订单处理 ------------------------------- */

    function openOrderModal(id) {
        var order = state.orders.find(function (item) {
            return item.id === id;
        });
        if (!order) {
            return;
        }
        state.handlingOrderId = id;
        dom.orderModalInfo.textContent = '订单 #' + order.id + ' · ' + order.carName + ' · 买家 '
            + order.buyerName + ' · ' + order.contactPhone;
        dom.orderStatusSelect.value = 'CONFIRMED';
        dom.orderReply.value = '';
        dom.orderModal.hidden = false;
    }

    function submitOrderHandle() {
        var id = state.handlingOrderId;
        if (!id) {
            return;
        }
        dom.orderSubmitBtn.disabled = true;
        postJson('/api/merchant/orders/' + id + '/status', {
            status: dom.orderStatusSelect.value,
            reply: dom.orderReply.value.trim()
        }).then(function () {
            dom.orderModal.hidden = true;
            toast('订单已处理，已通知买家', 'success');
            loadOrders();
            loadSummary();
        }).catch(function (error) {
            toast(error.message);
        }).finally(function () {
            dom.orderSubmitBtn.disabled = false;
        });
    }

    /* --------------------------- 实时消息（SSE） --------------------------- */

    function connectStream() {
        if (state.stream) {
            return;
        }
        if (!window.EventSource) {
            return;
        }
        var stream = new EventSource('/api/notifications/stream');
        stream.addEventListener('order', function (event) {
            var order = JSON.parse(event.data);
            toast('收到新的订单：' + order.carName + ' · ' + order.typeLabel + '（' + order.buyerName + '）', 'success');
            loadOrders();
            loadSummary();
        });
        stream.addEventListener('notification', function () {
            loadSummary();
        });
        stream.onerror = function () {
            /* 断线后浏览器会自动重连 */
        };
        state.stream = stream;
    }

    function openMessages() {
        dom.messageModal.hidden = false;
        dom.messageContent.innerHTML = '<p class="placeholder">正在加载消息…</p>';
        fetchJson('/api/notifications').then(function (list) {
            if (!list.length) {
                dom.messageContent.innerHTML = '<p class="placeholder">还没有消息</p>';
                return;
            }
            dom.messageContent.innerHTML = '<div class="message-list">' + list.map(function (item) {
                return '<div class="message-item' + (item.read ? '' : ' message-item--unread') + '">'
                    + '<div class="message-item__title">' + escapeHtml(item.title)
                    + '<small>' + escapeHtml((item.createdAt || '').replace('T', ' ').substring(0, 16)) + '</small></div>'
                    + '<div class="message-item__body">' + escapeHtml(item.content) + '</div></div>';
            }).join('') + '</div>';
            return postJson('/api/notifications/read-all', {});
        }).then(function () {
            loadSummary();
        }).catch(function (error) {
            dom.messageContent.innerHTML = '<p class="placeholder">加载失败：' + escapeHtml(error.message) + '</p>';
        });
    }

    document.addEventListener('DOMContentLoaded', init);
})();
