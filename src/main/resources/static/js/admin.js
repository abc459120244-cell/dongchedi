/* ==========================================================================
   管理后台脚本：用户列表、启用/禁用、角色调整
   ========================================================================== */
(function () {
    'use strict';

    var state = {
        user: null,
        users: [],
        keyword: ''
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
            return response.json();
        });
    }

    function postJson(url, body) {
        return fetchJson(url, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(body || {})
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
        }, 2800);
    }

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
        dom.userKeyword = $('#userKeyword');
        dom.refreshUsersBtn = $('#refreshUsersBtn');
        dom.userTableBody = $('#userTableBody');
        dom.toast = $('#toast');

        dom.loginForm.addEventListener('submit', onLogin);
        dom.refreshUsersBtn.addEventListener('click', function () {
            state.keyword = dom.userKeyword.value.trim().toLowerCase();
            renderUsers();
        });
        dom.userKeyword.addEventListener('keydown', function (event) {
            if (event.key === 'Enter') {
                event.preventDefault();
                state.keyword = dom.userKeyword.value.trim().toLowerCase();
                renderUsers();
            }
        });
        dom.userTableBody.addEventListener('click', function (event) {
            var action = event.target.closest('button[data-user-action]');
            if (!action) {
                return;
            }
            var id = Number(action.dataset.id);
            if (action.dataset.userAction === 'role') {
                changeRole(id);
            } else if (action.dataset.userAction === 'toggle') {
                toggleEnabled(id, action.dataset.enabled === 'true');
            }
        });

        loadSession();
    }

    function onLogin(event) {
        event.preventDefault();
        dom.loginError.hidden = true;
        dom.loginBtn.disabled = true;
        postJson('/api/auth/login', {
            username: dom.loginUsername.value.trim(),
            password: dom.loginPassword.value
        }).then(function (user) {
            if (user.role !== 'ADMIN') {
                throw new Error('该账号不是管理员，请使用 admin 账号登录');
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
    }

    function loadSession() {
        fetchJson('/api/auth/me').then(function (user) {
            if (user.role !== 'ADMIN') {
                showLogin('当前账号不是管理员，请使用管理员账号登录。');
                return;
            }
            state.user = user;
            enterWorkbench();
        }).catch(function () {
            showLogin();
        });
    }

    function showLogin(message) {
        state.user = null;
        dom.workbench.hidden = true;
        dom.loginSection.hidden = false;
        dom.userArea.innerHTML = '';
        if (message) {
            dom.loginError.textContent = message;
            dom.loginError.hidden = false;
        }
    }

    function enterWorkbench() {
        dom.loginSection.hidden = true;
        dom.workbench.hidden = false;
        renderUserArea();
        loadSummary();
        loadUsers();
    }

    function renderUserArea() {
        if (!state.user) {
            dom.userArea.innerHTML = '';
            return;
        }
        dom.userArea.innerHTML = '<span class="user-chip">'
            + '<span class="user-chip__avatar">管</span>'
            + '<span class="user-chip__name">' + escapeHtml(state.user.displayName) + '</span>'
            + '<em class="user-chip__role">管理员</em></span>'
            + '<button type="button" class="icon-btn" id="logoutBtn">退出</button>';
        $('#logoutBtn').addEventListener('click', function () {
            postJson('/api/auth/logout', {}).then(function () {
                state.user = null;
                dom.workbench.hidden = true;
                dom.loginSection.hidden = false;
                dom.userArea.innerHTML = '';
                toast('已退出登录');
            });
        });
    }

    function loadSummary() {
        fetchJson('/api/admin/summary').then(function (summary) {
            dom.statCards.innerHTML = [
                {value: summary.userCount, label: '用户总数'},
                {value: summary.merchantCount, label: '商家账号'},
                {value: summary.adminCount, label: '管理员'},
                {value: summary.disabledCount, label: '已禁用', alert: summary.disabledCount > 0}
            ].map(function (card) {
                return '<div class="stat-card' + (card.alert ? ' stat-card--alert' : '') + '">'
                    + '<div class="stat-card__value">' + card.value + '<small>个</small></div>'
                    + '<div class="stat-card__label">' + card.label + '</div></div>';
            }).join('');
        }).catch(function (error) {
            toast(error.message);
        });
    }

    function loadUsers() {
        return fetchJson('/api/admin/users').then(function (users) {
            state.users = users;
            renderUsers();
        }).catch(function (error) {
            toast(error.message);
        });
    }

    function renderUsers() {
        var users = state.users.filter(function (user) {
            if (!state.keyword) {
                return true;
            }
            return (user.username || '').toLowerCase().indexOf(state.keyword) >= 0
                || (user.nickname || '').toLowerCase().indexOf(state.keyword) >= 0;
        });
        if (!users.length) {
            dom.userTableBody.innerHTML = '<tr><td colspan="8" class="placeholder">没有匹配的用户</td></tr>';
            return;
        }
        dom.userTableBody.innerHTML = users.map(function (user) {
            var self = state.user && state.user.id === user.id;
            return '<tr>'
                + '<td>' + user.id + '</td>'
                + '<td><strong>' + escapeHtml(user.username) + '</strong></td>'
                + '<td>' + escapeHtml(user.nickname || '-') + '</td>'
                + '<td><select class="role-select" id="role-' + user.id + '"' + (self ? ' disabled' : '') + '>'
                + ['USER', 'MERCHANT', 'ADMIN'].map(function (role) {
                    return '<option value="' + role + '"' + (user.role === role ? ' selected' : '') + '>'
                        + ({USER: '普通用户', MERCHANT: '商家', ADMIN: '管理员'}[role]) + '</option>';
                }).join('')
                + '</select></td>'
                + '<td><span class="tag ' + (user.enabled ? 'tag--ok' : 'tag--off') + '">'
                + (user.enabled ? '正常' : '已禁用') + '</span></td>'
                + '<td>' + user.favoriteCount + '</td>'
                + '<td><small>' + escapeHtml((user.createdAt || '').replace('T', ' ').substring(0, 16)) + '</small></td>'
                + '<td class="table-actions">'
                + '<button type="button" class="icon-btn" data-user-action="role" data-id="' + user.id + '"'
                + (self ? ' disabled' : '') + '>保存角色</button>'
                + '<button type="button" class="icon-btn ' + (user.enabled ? 'icon-btn--danger' : '')
                + '" data-user-action="toggle" data-id="' + user.id + '" data-enabled="' + (!user.enabled) + '"'
                + (self ? ' disabled' : '') + '>' + (user.enabled ? '禁用' : '启用') + '</button>'
                + '</td></tr>';
        }).join('');
    }

    function changeRole(id) {
        var select = document.getElementById('role-' + id);
        if (!select) {
            return;
        }
        fetchJson('/api/admin/users/' + id + '/role?role=' + encodeURIComponent(select.value), {method: 'POST'})
            .then(function (user) {
                toast('已把 ' + user.username + ' 的角色改为「' + user.roleLabel + '」', 'success');
                loadSummary();
                loadUsers();
            }).catch(function (error) {
                toast(error.message);
            });
    }

    function toggleEnabled(id, enabled) {
        var user = state.users.find(function (item) {
            return item.id === id;
        });
        if (!user) {
            return;
        }
        var action = enabled ? '启用' : '禁用';
        if (!window.confirm('确定' + action + '账号「' + user.username + '」吗？')) {
            return;
        }
        fetchJson('/api/admin/users/' + id + '/enabled?enabled=' + enabled, {method: 'POST'})
            .then(function () {
                toast('已' + action + '账号 ' + user.username, 'success');
                loadSummary();
                loadUsers();
            }).catch(function (error) {
                toast(error.message);
            });
    }

    document.addEventListener('DOMContentLoaded', init);
})();
