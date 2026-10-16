/* ==========================================================================
   车帝选车 · 前端交互脚本（原生 JS，无第三方依赖）
   对应后端接口：
   GET  /api/cars            多条件筛选 + 排序 + 分页
   GET  /api/cars/filters    筛选项（含各选项车型数量）
   GET  /api/cars/compare    车型对比
   POST /api/recommend       智能推荐
   GET  /api/recommend/options 推荐表单选项
   GET  /api/statistics/*    市场统计与榜单
   ========================================================================== */
(function () {
    'use strict';

    var FAVORITE_KEY = 'dongchedi.favorites';

    var state = {
        filters: null,
        recommendOptions: null,
        query: {
            keyword: '',
            brand: [],
            level: [],
            bodyType: [],
            energyType: [],
            scene: [],
            minPrice: null,
            maxPrice: null,
            minSeats: null,
            minRange: null,
            maxFuel: null,
            minScore: null,
            maxAcceleration: null,
            sort: 'SALES_DESC',
            page: 1,
            size: 12
        },
        lastPage: null,
        favorites: new Set(loadFavorites()),
        favoriteOnly: false,
        compare: [],
        coverSeed: 0,
        user: null,
        authMode: 'login',
        rankingScope: 'ALL',
        orderCar: null,
        unread: 0,
        stream: null
    };

    var dom = {};

    /* ----------------------------- 工具函数 ----------------------------- */

    function $(selector) {
        return document.querySelector(selector);
    }

    function escapeHtml(value) {
        if (value === null || value === undefined) {
            return '';
        }
        return String(value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
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
            return response.json();
        });
    }

    var toastTimer = null;

    function toast(message) {
        dom.toast.textContent = message;
        dom.toast.hidden = false;
        if (toastTimer) {
            clearTimeout(toastTimer);
        }
        toastTimer = setTimeout(function () {
            dom.toast.hidden = true;
        }, 2200);
    }

    function loadFavorites() {
        try {
            var raw = window.localStorage.getItem(FAVORITE_KEY);
            return raw ? JSON.parse(raw) : [];
        } catch (error) {
            return [];
        }
    }

    function persistFavorites() {
        try {
            window.localStorage.setItem(FAVORITE_KEY, JSON.stringify(Array.from(state.favorites)));
        } catch (error) {
            /* 忽略隐私模式下的存储异常 */
        }
        dom.favoriteCount.textContent = String(state.favorites.size);
    }

    /** 根据品牌名生成稳定的封面渐变色，避免依赖外部图片资源 */
    function brandGradient(brand) {
        var hash = 0;
        var text = String(brand || 'car');
        for (var i = 0; i < text.length; i += 1) {
            hash = (hash * 31 + text.charCodeAt(i)) % 360;
        }
        var hue = (hash * 7 + 12) % 360;
        return 'linear-gradient(135deg, hsl(' + hue + ', 72%, 52%), hsl('
            + ((hue + 34) % 360) + ', 78%, 62%))';
    }

    /** 车身形式对应的矢量示意图路径（与后端 BodyType 枚举名小写一致） */
    function carPhoto(car) {
        return '/img/cars/' + String(car.bodyType || 'SEDAN').toLowerCase() + '.svg';
    }

    /** 品牌渐变底 + 车身示意图的小缩略图 */
    function carThumb(car, extraClass) {
        return '<span class="thumb' + (extraClass ? ' ' + extraClass : '')
            + '" style="background:' + brandGradient(car.brand) + '">'
            + '<img src="/img/cars/photos/' + car.id + '.jpg" alt="'
            + escapeHtml(car.bodyTypeLabel || '车型') + '" loading="lazy"'
            + ' onerror="this.onerror=null;this.src=\'' + carPhoto(car) + '\'"></span>';
    }

    /* ----------------------------- 初始化 ----------------------------- */

    function init() {
        dom.keywordInput = $('#keywordInput');
        dom.searchBtn = $('#searchBtn');
        dom.filterPanel = $('#filterPanel');
        dom.carGrid = $('#carGrid');
        dom.pagination = $('#pagination');
        dom.resultCount = $('#resultCount');
        dom.sortSelect = $('#sortSelect');
        dom.favoriteOnly = $('#favoriteOnly');
        dom.favoriteCount = $('#favoriteCount');
        dom.recommendForm = $('#recommendForm');
        dom.recommendResult = $('#recommendResult');
        dom.sceneChips = $('#sceneChips');
        dom.budgetPresets = $('#budgetPresets');
        dom.energyPreference = $('#energyPreference');
        dom.prioritySelect = $('#prioritySelect');
        dom.minSeatsSelect = $('#minSeatsSelect');
        dom.limitSelect = $('#limitSelect');
        dom.recMinPrice = $('#recMinPrice');
        dom.recMaxPrice = $('#recMaxPrice');
        dom.compareBar = $('#compareBar');
        dom.compareModal = $('#compareModal');
        dom.compareContent = $('#compareContent');
        dom.toast = $('#toast');

        dom.userArea = $('#userArea');
        dom.authModal = $('#authModal');
        dom.authForm = $('#authForm');
        dom.authUsername = $('#authUsername');
        dom.authPassword = $('#authPassword');
        dom.authNickname = $('#authNickname');
        dom.authNicknameRow = $('#authNicknameRow');
        dom.authError = $('#authError');
        dom.authHint = $('#authHint');
        dom.authSubmitBtn = $('#authSubmitBtn');
        dom.favoriteModal = $('#favoriteModal');
        dom.favoriteContent = $('#favoriteContent');
        dom.carRankingScope = $('#carRankingScope');
        dom.carRankingBody = $('#carRankingTable tbody');
        dom.orderCreateModal = $('#orderCreateModal');
        dom.orderCarInfo = $('#orderCarInfo');
        dom.orderTypeSelect = $('#orderTypeSelect');
        dom.orderContactName = $('#orderContactName');
        dom.orderContactPhone = $('#orderContactPhone');
        dom.orderRemark = $('#orderRemark');
        dom.orderError = $('#orderError');
        dom.orderSubmitBtn = $('#orderSubmitBtn');
        dom.myOrderModal = $('#myOrderModal');
        dom.myOrderContent = $('#myOrderContent');

        // 先用未登录状态渲染顶栏，避免接口返回前右上角是空白
        renderUserArea();
        bindEvents();
        loadFilters();
        loadRecommendOptions();
        loadStatistics();
        loadCars();
        loadSession();
    }

    function bindEvents() {
        dom.searchBtn.addEventListener('click', function () {
            state.query.keyword = dom.keywordInput.value.trim();
            state.query.page = 1;
            loadCars();
        });

        dom.keywordInput.addEventListener('keydown', function (event) {
            if (event.key === 'Enter') {
                event.preventDefault();
                dom.searchBtn.click();
            }
        });

        dom.filterPanel.addEventListener('click', function (event) {
            var chip = event.target.closest('.chip[data-group]');
            if (chip) {
                toggleFilter(chip.dataset.group, chip.dataset.value);
                return;
            }
            if (event.target.closest('#resetFilters')) {
                resetFilters();
            }
        });

        dom.filterPanel.addEventListener('change', function (event) {
            if (event.target.matches('[data-range]')) {
                applyRangeInputs();
            }
        });

        dom.sortSelect.addEventListener('change', function () {
            state.query.sort = dom.sortSelect.value;
            state.query.page = 1;
            loadCars();
        });

        dom.favoriteOnly.addEventListener('change', function () {
            state.favoriteOnly = dom.favoriteOnly.checked;
            renderCars(state.lastPage);
        });

        dom.carGrid.addEventListener('click', function (event) {
            var favoriteBtn = event.target.closest('[data-action="favorite"]');
            if (favoriteBtn) {
                toggleFavorite(Number(favoriteBtn.dataset.id));
                return;
            }
            var compareBtn = event.target.closest('[data-action="compare"]');
            if (compareBtn) {
                toggleCompare(Number(compareBtn.dataset.id));
                return;
            }
            var orderBtn = event.target.closest('[data-action="order"]');
            if (orderBtn) {
                requestOrder(Number(orderBtn.dataset.id));
            }
        });

        dom.orderCreateModal.addEventListener('click', function (event) {
            if (event.target.closest('[data-order-create-close]')) {
                closeOrderModal();
            }
        });
        dom.orderSubmitBtn.addEventListener('click', submitOrder);
        dom.myOrderModal.addEventListener('click', function (event) {
            if (event.target.closest('[data-my-order-close]')) {
                dom.myOrderModal.hidden = true;
            }
        });

        dom.pagination.addEventListener('click', function (event) {
            var button = event.target.closest('button[data-page]');
            if (!button || button.disabled) {
                return;
            }
            state.query.page = Number(button.dataset.page);
            loadCars();
        });

        dom.recommendForm.addEventListener('submit', function (event) {
            event.preventDefault();
            runRecommend();
        });

        $('#resetRecommendBtn').addEventListener('click', function () {
            dom.recMinPrice.value = 12;
            dom.recMaxPrice.value = 20;
            dom.energyPreference.value = 'ANY';
            dom.prioritySelect.value = 'COMPREHENSIVE';
            dom.minSeatsSelect.value = '5';
            dom.limitSelect.value = '5';
            Array.prototype.forEach.call(dom.sceneChips.querySelectorAll('.chip'), function (chip) {
                chip.classList.remove('is-active');
            });
            dom.recommendResult.innerHTML = '<p class="placeholder">还没有推荐结果，点击「开始推荐」试试。</p>';
        });

        dom.sceneChips.addEventListener('click', function (event) {
            var chip = event.target.closest('.chip');
            if (chip) {
                chip.classList.toggle('is-active');
            }
        });

        dom.budgetPresets.addEventListener('click', function (event) {
            var button = event.target.closest('button[data-min]');
            if (!button) {
                return;
            }
            dom.recMinPrice.value = button.dataset.min;
            dom.recMaxPrice.value = button.dataset.max;
        });

        dom.compareBar.addEventListener('click', function (event) {
            if (event.target.closest('[data-compare-clear]')) {
                state.compare = [];
                renderCompareBar();
                return;
            }
            if (event.target.closest('[data-compare-open]')) {
                openCompare();
                return;
            }
            var removeBtn = event.target.closest('[data-compare-remove]');
            if (removeBtn) {
                state.compare = state.compare.filter(function (car) {
                    return car.id !== Number(removeBtn.dataset.compareRemove);
                });
                renderCompareBar();
                renderCars(state.lastPage);
            }
        });

        dom.compareModal.addEventListener('click', function (event) {
            if (event.target.closest('[data-close]')) {
                closeCompare();
            }
        });

        dom.userArea.addEventListener('click', function (event) {
            var action = event.target.closest('[data-user-action]');
            if (!action) {
                return;
            }
            if (action.dataset.userAction === 'login') {
                openAuthModal('login');
            } else if (action.dataset.userAction === 'logout') {
                logout();
            } else if (action.dataset.userAction === 'favorites') {
                openFavoriteModal();
            } else if (action.dataset.userAction === 'orders') {
                openMyOrders();
            }
        });

        dom.authModal.addEventListener('click', function (event) {
            if (event.target.closest('[data-auth-close]')) {
                closeAuthModal();
                return;
            }
            var tab = event.target.closest('[data-auth-tab]');
            if (tab) {
                setAuthMode(tab.dataset.authTab);
            }
        });

        dom.authForm.addEventListener('submit', submitAuth);

        dom.favoriteModal.addEventListener('click', function (event) {
            if (event.target.closest('[data-favorite-close]')) {
                closeFavoriteModal();
                return;
            }
            var removeBtn = event.target.closest('[data-favorite-remove]');
            if (removeBtn) {
                removeFavorite(Number(removeBtn.dataset.favoriteRemove));
            }
        });

        // 首页「只看性能车」快捷入口：筛选零百 5 秒内的车型并按加速排序
        dom.carRankingScope.addEventListener('click', function (event) {
            var button = event.target.closest('button[data-scope]');
            if (!button) {
                return;
            }
            Array.prototype.forEach.call(dom.carRankingScope.querySelectorAll('button'), function (item) {
                item.classList.toggle('is-active', item === button);
            });
            loadCarRanking(button.dataset.scope);
        });

        $('#performanceShortcut').addEventListener('click', function () {
            state.query.scene = ['PERFORMANCE'];
            state.query.maxAcceleration = 5;
            state.query.sort = 'ACCELERATION_ASC';
            state.query.page = 1;
            renderFilterPanel();
            if (state.filters) {
                dom.sortSelect.value = state.query.sort;
            }
            loadCars();
            document.getElementById('cars').scrollIntoView({behavior: 'smooth'});
            toast('已筛选零百 5 秒内的性能车，并按加速从快到慢排序');
        });

        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape') {
                closeCompare();
                closeAuthModal();
                closeFavoriteModal();
            }
        });
    }

    /* ----------------------------- 筛选面板 ----------------------------- */

    function loadFilters() {
        fetchJson('/api/cars/filters').then(function (filters) {
            state.filters = filters;
            renderFilterPanel();
            renderSortSelect();
        }).catch(function (error) {
            toast(error.message);
        });
    }

    function renderSortSelect() {
        dom.sortSelect.innerHTML = state.filters.sorts.map(function (item) {
            return '<option value="' + escapeHtml(item.code) + '">' + escapeHtml(item.label) + '</option>';
        }).join('');
        dom.sortSelect.value = state.query.sort;
    }

    function renderFilterPanel() {
        var filters = state.filters;
        var html = '';

        html += '<div class="filters__head"><h3>筛选条件</h3>'
            + '<button type="button" class="link-btn" id="resetFilters">清空全部</button></div>';

        html += chipGroup('品牌', 'brand', filters.brands);
        html += chipGroup('级别', 'level', filters.levels);
        html += chipGroup('车身形式', 'bodyType', filters.bodyTypes);
        html += chipGroup('能源类型', 'energyType', filters.energyTypes);
        html += chipGroup('用车场景', 'scene', filters.scenes);

        html += '<div class="filter-group"><div class="filter-group__title">价格区间（万元）</div>'
            + '<div class="range-inputs">'
            + '<input type="number" data-range="minPrice" placeholder="最低" min="0" step="0.5" value="'
            + (state.query.minPrice === null ? '' : state.query.minPrice) + '">'
            + '<span class="dash">—</span>'
            + '<input type="number" data-range="maxPrice" placeholder="最高" min="0" step="0.5" value="'
            + (state.query.maxPrice === null ? '' : state.query.maxPrice) + '">'
            + '</div><div class="filter-group__title" style="margin-top:6px">'
            + '<span>数据集范围 ' + filters.minPrice + ' ~ ' + filters.maxPrice + ' 万</span></div></div>';

        html += '<div class="filter-group"><div class="filter-group__title">座位数（至少）</div>'
            + '<div class="filter-chips">'
            + filters.seatOptions.map(function (seat) {
                return '<button type="button" class="chip' + (state.query.minSeats === seat ? ' is-active' : '')
                    + '" data-group="minSeats" data-value="' + seat + '">' + seat + ' 座</button>';
            }).join('')
            + '</div></div>';

        html += '<div class="filter-group"><div class="filter-group__title">纯电续航（至少 km）</div>'
            + '<div class="range-inputs"><input type="number" data-range="minRange" placeholder="例如 500" min="0" '
            + 'step="50" value="' + (state.query.minRange === null ? '' : state.query.minRange) + '"></div></div>';

        html += '<div class="filter-group"><div class="filter-group__title">百公里油耗（不超过 L）</div>'
            + '<div class="range-inputs"><input type="number" data-range="maxFuel" placeholder="例如 6" min="0" '
            + 'step="0.5" value="' + (state.query.maxFuel === null ? '' : state.query.maxFuel) + '"></div></div>';

        html += '<div class="filter-group"><div class="filter-group__title">口碑评分（不低于）</div>'
            + '<div class="filter-chips">'
            + [4.0, 4.2, 4.4, 4.5].map(function (score) {
                return '<button type="button" class="chip' + (state.query.minScore === score ? ' is-active' : '')
                    + '" data-group="minScore" data-value="' + score + '">' + score + ' 分</button>';
            }).join('')
            + '</div></div>';

        html += '<div class="filter-group"><div class="filter-group__title">零百加速（不超过）</div>'
            + '<div class="filter-chips">'
            + [3, 4, 5, 7].map(function (seconds) {
                return '<button type="button" class="chip' + (state.query.maxAcceleration === seconds ? ' is-active' : '')
                    + '" data-group="maxAcceleration" data-value="' + seconds + '">' + seconds + ' 秒内</button>';
            }).join('')
            + '</div></div>';

        dom.filterPanel.innerHTML = html;
    }

    function chipGroup(title, group, options) {
        var html = '<div class="filter-group"><div class="filter-group__title">' + escapeHtml(title)
            + '<span>' + options.length + ' 项</span></div><div class="filter-chips">';
        html += options.map(function (option) {
            var active = state.query[group].indexOf(option.code) >= 0;
            return '<button type="button" class="chip' + (active ? ' is-active' : '')
                + '" data-group="' + group + '" data-value="' + escapeHtml(option.code) + '">'
                + escapeHtml(option.label) + '<small>' + option.count + '</small></button>';
        }).join('');
        html += '</div></div>';
        return html;
    }

    function toggleFilter(group, rawValue) {
        if (group === 'minSeats' || group === 'minScore' || group === 'maxAcceleration') {
            var value = Number(rawValue);
            state.query[group] = state.query[group] === value ? null : value;
        } else {
            var list = state.query[group].slice();
            var index = list.indexOf(rawValue);
            if (index >= 0) {
                list.splice(index, 1);
            } else {
                list.push(rawValue);
            }
            state.query[group] = list;
        }
        state.query.page = 1;
        renderFilterPanel();
        loadCars();
    }

    function applyRangeInputs() {
        var mapping = {
            minPrice: 'minPrice',
            maxPrice: 'maxPrice',
            minRange: 'minRange',
            maxFuel: 'maxFuel'
        };
        Array.prototype.forEach.call(dom.filterPanel.querySelectorAll('[data-range]'), function (input) {
            var key = mapping[input.dataset.range];
            var raw = input.value.trim();
            state.query[key] = raw === '' ? null : Number(raw);
        });
        state.query.page = 1;
        loadCars();
    }

    function resetFilters() {
        state.query.keyword = '';
        dom.keywordInput.value = '';
        ['brand', 'level', 'bodyType', 'energyType', 'scene'].forEach(function (key) {
            state.query[key] = [];
        });
        ['minPrice', 'maxPrice', 'minSeats', 'minRange', 'maxFuel', 'minScore', 'maxAcceleration']
            .forEach(function (key) {
            state.query[key] = null;
        });
        state.query.page = 1;
        renderFilterPanel();
        loadCars();
        toast('已清空筛选条件');
    }

    /* ----------------------------- 车型列表 ----------------------------- */

    function buildQueryParams() {
        var params = new URLSearchParams();
        var query = state.query;
        if (query.keyword) {
            params.set('keyword', query.keyword);
        }
        ['brand', 'level', 'bodyType', 'energyType', 'scene'].forEach(function (key) {
            query[key].forEach(function (value) {
                params.append(key, value);
            });
        });
        ['minPrice', 'maxPrice', 'minSeats', 'minRange', 'maxFuel', 'minScore', 'maxAcceleration']
            .forEach(function (key) {
            if (query[key] !== null && query[key] !== '' && !Number.isNaN(query[key])) {
                params.set(key, query[key]);
            }
        });
        params.set('sort', query.sort);
        params.set('page', query.page);
        params.set('size', query.size);
        return params;
    }

    function loadCars() {
        dom.carGrid.innerHTML = '<div class="empty">正在加载车型数据…</div>';
        fetchJson('/api/cars?' + buildQueryParams().toString()).then(function (page) {
            state.lastPage = page;
            renderCars(page);
            renderPagination(page);
        }).catch(function (error) {
            dom.carGrid.innerHTML = '<div class="empty">加载失败：' + escapeHtml(error.message) + '</div>';
        });
    }

    function renderCars(page) {
        if (!page) {
            return;
        }
        var items = page.items;
        if (state.favoriteOnly) {
            items = items.filter(function (car) {
                return state.favorites.has(car.id);
            });
        }

        dom.resultCount.innerHTML = '共筛出 <b>' + page.total + '</b> 款车型，第 ' + page.page + ' / '
            + Math.max(page.totalPages, 1) + ' 页'
            + (state.favoriteOnly ? '（当前页收藏 ' + items.length + ' 款）' : '');

        if (!items.length) {
            dom.carGrid.innerHTML = '<div class="empty">'
                + (state.favoriteOnly ? '当前页没有收藏车型，试试翻页或取消「只看收藏」。' : '没有符合条件的车型，请放宽筛选条件。')
                + '</div>';
            return;
        }

        dom.carGrid.innerHTML = items.map(renderCarCard).join('');
    }

    function renderCarCard(car) {
        var favorite = state.favorites.has(car.id);
        var compared = state.compare.some(function (item) {
            return item.id === car.id;
        });
        var badges = '';
        if (car.performanceCar) {
            badges += '<span class="badge badge--performance">性能车</span>';
        }
        if (car.newEnergy) {
            badges += '<span class="badge badge--newenergy">新能源</span>';
        }
        badges += '<span class="badge">' + escapeHtml(car.energyTypeLabel) + '</span>';

        var tags = (car.tags || []).slice(0, 3).map(function (tag) {
            return '<span class="tag">' + escapeHtml(tag) + '</span>';
        }).join('');

        return '<article class="car-card">'
            + '<div class="car-card__cover" style="background:' + brandGradient(car.brand) + '">'
            + '<img class="car-card__bg" src="/img/cars/photos/' + car.id + '.jpg" alt="" loading="lazy"'
            + ' onload="this.parentNode.classList.add(\'has-photo\')" onerror="this.remove()">'
            + '<div class="car-card__badges">' + badges + '</div>'
            + '<img class="car-card__art" src="' + carPhoto(car) + '" alt="'
            + escapeHtml(car.bodyTypeLabel) + ' 示意图" loading="lazy">'
            + '<div class="car-card__caption">'
            + '<div class="car-card__brand">' + escapeHtml(car.brand) + ' · ' + escapeHtml(car.bodyTypeLabel) + '</div>'
            + '<div class="car-card__name">' + escapeHtml(car.name) + '</div>'
            + '</div>'
            + '</div>'
            + '<div class="car-card__body">'
            + '<div class="car-card__price"><strong>' + num(car.price).toFixed(2) + ' 万</strong>'
            + '<small>' + escapeHtml(car.levelLabel) + ' · ' + car.seats + ' 座</small></div>'
            + '<div class="car-card__meta">' + tags + '</div>'
            + '<p class="car-card__desc">' + escapeHtml(car.highlight) + '</p>'
            + '<div class="car-card__meta"><span class="tag">' + escapeHtml(car.energyDescription) + '</span>'
            + '<span class="tag">轴距 ' + car.wheelbaseMm + 'mm</span>'
            + '<span class="tag">' + car.maxPowerKw + 'kW</span>'
            + '<span class="tag">' + escapeHtml(car.accelerationLabel) + '</span></div>'
            + '<div class="car-card__foot">'
            + '<span class="car-card__score"><b>★ ' + num(car.userScore).toFixed(1) + '</b> · 月销 '
            + num(car.monthlySales).toLocaleString('zh-CN') + '</span>'
            + '<span class="card-actions">'
            + '<button type="button" class="icon-btn' + (favorite ? ' is-active' : '')
            + '" data-action="favorite" data-id="' + car.id + '">' + (favorite ? '已收藏' : '收藏') + '</button>'
            + '<button type="button" class="icon-btn" data-action="order" data-id="' + car.id + '">预约</button>'
            + '<button type="button" class="icon-btn' + (compared ? ' is-active' : '')
            + '" data-action="compare" data-id="' + car.id + '">' + (compared ? '已选对比' : '对比') + '</button>'
            + '</span></div></div></article>';
    }

    function renderPagination(page) {
        var html = '';
        if (page.totalPages <= 1) {
            dom.pagination.innerHTML = '';
            return;
        }
        html += '<button type="button" data-page="' + (page.page - 1) + '"'
            + (page.page <= 1 ? ' disabled' : '') + '>上一页</button>';
        for (var i = 1; i <= page.totalPages; i += 1) {
            html += '<button type="button" data-page="' + i + '"'
                + (i === page.page ? ' class="is-active"' : '') + '>' + i + '</button>';
        }
        html += '<button type="button" data-page="' + (page.page + 1) + '"'
            + (page.hasNext ? '' : ' disabled') + '>下一页</button>';
        dom.pagination.innerHTML = html;
    }

    /* ----------------------------- 收藏与对比 ----------------------------- */

    function toggleFavorite(id) {
        if (!state.user) {
            // 未登录：临时保存在浏览器本地，并引导用户登录
            if (state.favorites.has(id)) {
                state.favorites.delete(id);
            } else {
                state.favorites.add(id);
            }
            applyFavoritesChange();
            renderCars(state.lastPage);
            toast('已临时收藏到本机，登录后可保存到账号');
            if (!state.authPrompted) {
                state.authPrompted = true;
                openAuthModal('login', '收藏功能需要登录：登录后收藏会保存到你的账号，可在右上角「我的收藏」中查看。');
            }
            return;
        }
        var favorited = state.favorites.has(id);
        fetchJson('/api/favorites/' + id, {method: favorited ? 'DELETE' : 'POST'})
            .then(function (result) {
                state.favorites = new Set(result.ids);
                applyFavoritesChange();
                renderCars(state.lastPage);
                toast(result.favorited ? '已加入收藏' : '已取消收藏');
            })
            .catch(function (error) {
                toast(error.message);
            });
    }

    function toggleCompare(id) {
        var exists = state.compare.some(function (car) {
            return car.id === id;
        });
        if (exists) {
            state.compare = state.compare.filter(function (car) {
                return car.id !== id;
            });
        } else {
            if (state.compare.length >= 4) {
                toast('最多同时对比 4 款车型');
                return;
            }
            var car = findCar(id);
            if (car) {
                state.compare.push(car);
            }
        }
        renderCompareBar();
        renderCars(state.lastPage);
    }

    /* ----------------------------- 登录与收藏 ----------------------------- */

    function loadSession() {
        fetchJson('/api/auth/me')
            .then(function (user) {
                state.user = user;
                return loadServerFavorites();
            })
            .catch(function () {
                // 未登录属于正常情况，回退到浏览器本地收藏
                state.user = null;
                state.favorites = new Set(loadFavorites());
                applyFavoritesChange();
                renderCars(state.lastPage);
            });
    }

    function loadServerFavorites() {
        return fetchJson('/api/favorites/ids').then(function (ids) {
            state.favorites = new Set(ids);
            applyFavoritesChange();
            renderCars(state.lastPage);
            loadUnread();
            connectStream();
        });
    }

    function applyFavoritesChange() {
        dom.favoriteCount.textContent = String(state.favorites.size);
        if (!state.user) {
            // 未登录时收藏暂存在浏览器本地
            persistFavorites();
        }
        renderUserArea();
    }

    function renderUserArea() {
        if (!state.user) {
            dom.userArea.innerHTML = '<button type="button" class="btn btn--primary btn--small"'
                + ' data-user-action="login">登录 / 注册</button>';
            return;
        }
        var name = state.user.displayName || state.user.username;
        dom.userArea.innerHTML = '<span class="user-chip">'
            + '<span class="user-chip__avatar">' + escapeHtml(name.substring(0, 1).toUpperCase()) + '</span>'
            + '<span class="user-chip__name">' + escapeHtml(name) + '</span>'
            + (state.user.role === 'ADMIN' ? '<em class="user-chip__role">管理员</em>' : '')
            + '</span>'
            + '<button type="button" class="icon-btn" data-user-action="favorites">我的收藏 (<em>'
            + state.favorites.size + '</em>)</button>'
            + '<button type="button" class="icon-btn" data-user-action="orders">我的订单'
            + (state.unread > 0 ? ' <em>' + state.unread + '</em>' : '') + '</button>'
            + '<button type="button" class="icon-btn" data-user-action="logout">退出</button>';
    }

    function openAuthModal(mode, hint) {
        setAuthMode(mode === 'register' ? 'register' : 'login');
        if (hint) {
            dom.authHint.textContent = hint;
            dom.authHint.hidden = false;
        } else {
            dom.authHint.hidden = true;
        }
        dom.authModal.hidden = false;
        dom.authUsername.focus();
    }

    function setAuthMode(mode) {
        state.authMode = mode === 'register' ? 'register' : 'login';
        Array.prototype.forEach.call(dom.authModal.querySelectorAll('.auth-tab'), function (tab) {
            tab.classList.toggle('is-active', tab.dataset.authTab === state.authMode);
        });
        dom.authNicknameRow.hidden = state.authMode !== 'register';
        dom.authSubmitBtn.textContent = state.authMode === 'register' ? '注册并登录' : '登录';
        dom.authError.hidden = true;
    }

    function closeAuthModal() {
        dom.authModal.hidden = true;
        dom.authError.hidden = true;
    }

    function submitAuth(event) {
        event.preventDefault();
        var payload = {
            username: dom.authUsername.value.trim(),
            password: dom.authPassword.value
        };
        if (state.authMode === 'register') {
            payload.nickname = dom.authNickname.value.trim();
        }
        var mode = state.authMode;
        dom.authSubmitBtn.disabled = true;
        dom.authError.hidden = true;

        fetchJson('/api/auth/' + mode, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        }).then(function (user) {
            state.user = user;
            dom.authPassword.value = '';
            closeAuthModal();
            toast((mode === 'register' ? '注册成功，已自动登录：' : '登录成功，欢迎回来 ')
                + (user.displayName || user.username));
            return loadServerFavorites();
        }).catch(function (error) {
            dom.authError.textContent = error.message;
            dom.authError.hidden = false;
        }).finally(function () {
            dom.authSubmitBtn.disabled = false;
        });
    }

    function logout() {
        fetchJson('/api/auth/logout', {method: 'POST'}).then(function () {
            state.user = null;
            state.unread = 0;
            if (state.stream) {
                state.stream.close();
                state.stream = null;
            }
            state.favorites = new Set(loadFavorites());
            applyFavoritesChange();
            renderCars(state.lastPage);
            toast('已退出登录');
        }).catch(function (error) {
            toast(error.message);
        });
    }

    function openFavoriteModal() {
        dom.favoriteModal.hidden = false;
        dom.favoriteContent.innerHTML = '<p class="placeholder">正在加载收藏…</p>';
        fetchJson('/api/favorites').then(renderFavoriteList).catch(function (error) {
            dom.favoriteContent.innerHTML = '<p class="placeholder">加载失败：'
                + escapeHtml(error.message) + '</p>';
        });
    }

    function renderFavoriteList(cars) {
        if (!cars || !cars.length) {
            dom.favoriteContent.innerHTML = '<p class="placeholder">还没有收藏车型，'
                + '在车型卡片上点击「收藏」即可加入。</p>';
            return;
        }
        dom.favoriteContent.innerHTML = '<div class="favorite-list">'
            + cars.map(function (car) {
                return '<div class="favorite-item">'
                    + '<div class="favorite-item__main">' + carThumb(car)
                    + '<div><strong>' + escapeHtml(car.name) + '</strong>'
                    + '<div class="favorite-item__meta">' + num(car.price).toFixed(2) + ' 万 · '
                    + escapeHtml(car.energyTypeLabel) + ' · ' + escapeHtml(car.levelLabel) + ' · ★ '
                    + num(car.userScore).toFixed(1) + '</div></div></div>'
                    + '<button type="button" class="icon-btn" data-favorite-remove="' + car.id + '">移除</button>'
                    + '</div>';
            }).join('')
            + '</div>';
    }

    function removeFavorite(id) {
        fetchJson('/api/favorites/' + id, {method: 'DELETE'}).then(function (result) {
            state.favorites = new Set(result.ids);
            applyFavoritesChange();
            renderCars(state.lastPage);
            return fetchJson('/api/favorites');
        }).then(renderFavoriteList).catch(function (error) {
            toast(error.message);
        });
    }

    function closeFavoriteModal() {
        dom.favoriteModal.hidden = true;
        dom.favoriteContent.innerHTML = '';
    }

    /* ----------------------------- 下单与我的订单 ----------------------------- */

    function requestOrder(carId) {
        var car = findCar(carId);
        if (!state.user) {
            openAuthModal('login', '预约试驾 / 询价需要登录：登录后提交的订单会实时推送给对应商家。');
            return;
        }
        if (!car) {
            return;
        }
        state.orderCar = car;
        dom.orderCarInfo.textContent = car.name + ' · ' + num(car.price).toFixed(2) + ' 万 · '
            + car.brand + ' · ' + car.energyTypeLabel;
        dom.orderContactName.value = state.user.displayName || state.user.username;
        dom.orderContactPhone.value = '';
        dom.orderRemark.value = '';
        dom.orderError.hidden = true;
        dom.orderCreateModal.hidden = false;
    }

    function closeOrderModal() {
        dom.orderCreateModal.hidden = true;
        state.orderCar = null;
    }

    function submitOrder() {
        if (!state.orderCar) {
            return;
        }
        var payload = {
            carId: state.orderCar.id,
            type: dom.orderTypeSelect.value,
            contactName: dom.orderContactName.value.trim(),
            contactPhone: dom.orderContactPhone.value.trim(),
            remark: dom.orderRemark.value.trim()
        };
        dom.orderSubmitBtn.disabled = true;
        dom.orderError.hidden = true;
        fetchJson('/api/orders', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        }).then(function (order) {
            closeOrderModal();
            toast('已提交「' + order.typeLabel + '」，商家「' + (order.dealerName || '平台自营')
                + '」已收到实时提醒', 'success');
        }).catch(function (error) {
            dom.orderError.textContent = error.message;
            dom.orderError.hidden = false;
        }).finally(function () {
            dom.orderSubmitBtn.disabled = false;
        });
    }

    function openMyOrders() {
        dom.myOrderModal.hidden = false;
        dom.myOrderContent.innerHTML = '<p class="placeholder">正在加载订单…</p>';
        fetchJson('/api/orders/mine').then(function (orders) {
            if (!orders.length) {
                dom.myOrderContent.innerHTML = '<p class="placeholder">还没有订单，去车型卡片点「预约」试试。</p>';
                return null;
            }
            dom.myOrderContent.innerHTML = '<div class="message-list">' + orders.map(function (order) {
                return '<div class="message-item">'
                    + '<div class="message-item__title"><span>#' + order.id + ' '
                    + escapeHtml(order.carName) + '</span>'
                    + '<span class="tag ' + orderStatusClass(order.status) + '">'
                    + escapeHtml(order.statusLabel) + '</span></div>'
                    + '<div class="message-item__body">' + escapeHtml(order.typeLabel)
                    + ' · 商家：' + escapeHtml(order.dealerName)
                    + ' · 提交时间 ' + escapeHtml((order.createdAt || '').replace('T', ' ').substring(0, 16))
                    + (order.dealerReply ? '<br>商家回复：' + escapeHtml(order.dealerReply) : '')
                    + '</div></div>';
            }).join('') + '</div>';
            return fetchJson('/api/notifications/read-all', {method: 'POST'});
        }).then(function () {
            loadUnread();
        }).catch(function (error) {
            dom.myOrderContent.innerHTML = '<p class="placeholder">加载失败：'
                + escapeHtml(error.message) + '</p>';
        });
    }

    function orderStatusClass(status) {
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

    function loadUnread() {
        if (!state.user) {
            state.unread = 0;
            return Promise.resolve();
        }
        return fetchJson('/api/notifications/unread-count').then(function (result) {
            state.unread = result.unread || 0;
            renderUserArea();
        }).catch(function () {
            /* 忽略 */
        });
    }

    /** 买家端也订阅 SSE：商家处理订单后可以实时收到状态变化 */
    function connectStream() {
        if (state.stream || !window.EventSource) {
            return;
        }
        var stream = new EventSource('/api/notifications/stream');
        stream.addEventListener('order', function (event) {
            var order = JSON.parse(event.data);
            toast('订单 #' + order.id + ' 状态更新：' + order.statusLabel
                + (order.dealerReply ? '（商家回复：' + order.dealerReply + '）' : ''), 'success');
            loadUnread();
            if (!dom.myOrderModal.hidden) {
                openMyOrders();
            }
        });
        stream.addEventListener('notification', function () {
            loadUnread();
        });
        stream.onerror = function () {
            /* 断线后浏览器会自动重连 */
        };
        state.stream = stream;
    }

    function findCar(id) {
        if (!state.lastPage) {
            return null;
        }
        return state.lastPage.items.find(function (car) {
            return car.id === id;
        }) || null;
    }

    function renderCompareBar() {
        if (state.compare.length === 0) {
            dom.compareBar.hidden = true;
            dom.compareBar.innerHTML = '';
            return;
        }
        dom.compareBar.hidden = false;
        dom.compareBar.innerHTML = '<span>已选 ' + state.compare.length + ' 款：</span>'
            + '<span class="compare-bar__list">'
            + state.compare.map(function (car) {
                return '<span class="compare-bar__item">' + escapeHtml(car.name)
                    + '<button type="button" data-compare-remove="' + car.id + '">×</button></span>';
            }).join('')
            + '</span>'
            + '<button type="button" class="btn btn--primary btn--small" data-compare-open="true"'
            + (state.compare.length < 2 ? ' disabled' : '') + '>开始对比</button>'
            + '<button type="button" class="btn btn--ghost btn--small" data-compare-clear="true">清空</button>';
    }

    function openCompare() {
        if (state.compare.length < 2) {
            toast('至少选择 2 款车型才能对比');
            return;
        }
        var ids = state.compare.map(function (car) {
            return car.id;
        }).join(',');
        dom.compareContent.innerHTML = '<p class="placeholder">正在加载对比数据…</p>';
        dom.compareModal.hidden = false;
        fetchJson('/api/cars/compare?ids=' + encodeURIComponent(ids)).then(function (cars) {
            dom.compareContent.innerHTML = renderCompareTable(cars);
        }).catch(function (error) {
            dom.compareContent.innerHTML = '<p class="placeholder">对比数据加载失败：'
                + escapeHtml(error.message) + '</p>';
        });
    }

    function closeCompare() {
        dom.compareModal.hidden = true;
        dom.compareContent.innerHTML = '';
    }

    function renderCompareTable(cars) {
        var rows = [
            {label: '车型', value: function (car) { return '<strong>' + escapeHtml(car.name) + '</strong>'; }},
            {label: '厂商指导价', value: function (car) { return num(car.price).toFixed(2) + ' 万'; },
                best: function (list) { return minIndex(list, function (car) { return num(car.price); }); }},
            {label: '能源类型', value: function (car) { return escapeHtml(car.energyTypeLabel); }},
            {label: '级别 / 车身', value: function (car) { return escapeHtml(car.levelLabel) + ' / ' + escapeHtml(car.bodyTypeLabel); }},
            {label: '座位数', value: function (car) { return car.seats + ' 座'; },
                best: function (list) { return maxIndex(list, function (car) { return car.seats; }); }},
            {label: '长×宽×高 (mm)', value: function (car) {
                return car.lengthMm + '×' + car.widthMm + '×' + car.heightMm; }},
            {label: '轴距 (mm)', value: function (car) { return car.wheelbaseMm; },
                best: function (list) { return maxIndex(list, function (car) { return car.wheelbaseMm; }); }},
            {label: '最大功率 (kW)', value: function (car) { return car.maxPowerKw; },
                best: function (list) { return maxIndex(list, function (car) { return car.maxPowerKw; }); }},
            {label: '零百加速 (s)', value: function (car) { return num(car.zeroToHundredSeconds).toFixed(1); },
                best: function (list) {
                    return minIndex(list, function (car) { return num(car.zeroToHundredSeconds); });
                }},
            {label: '性能车', value: function (car) { return car.performanceCar ? '是' : '否'; }},
            {label: '能耗表现', value: function (car) { return escapeHtml(car.energyDescription); }},
            {label: '月销量', value: function (car) { return num(car.monthlySales).toLocaleString('zh-CN'); },
                best: function (list) { return maxIndex(list, function (car) { return car.monthlySales; }); }},
            {label: '用户口碑', value: function (car) { return '★ ' + num(car.userScore).toFixed(1); },
                best: function (list) { return maxIndex(list, function (car) { return car.userScore; }); }},
            {label: '标签', value: function (car) {
                return (car.tags || []).map(function (tag) { return escapeHtml(tag); }).join('、'); }},
            {label: '一句话卖点', value: function (car) { return escapeHtml(car.highlight); }}
        ];

        var html = '<table class="compare-table"><thead><tr><th>对比项</th>'
            + cars.map(function (car) {
                return '<th>' + escapeHtml(car.name) + '</th>';
            }).join('') + '</tr></thead><tbody>';

        rows.forEach(function (row) {
            var bestIndex = row.best ? row.best(cars) : -1;
            html += '<tr><th>' + row.label + '</th>';
            cars.forEach(function (car, index) {
                html += '<td' + (index === bestIndex && cars.length > 1 ? ' class="best"' : '') + '>'
                    + row.value(car) + '</td>';
            });
            html += '</tr>';
        });

        html += '</tbody></table>';
        return html;
    }

    function maxIndex(list, score) {
        var best = -1;
        var bestValue = -Infinity;
        list.forEach(function (item, index) {
            var value = num(score(item));
            if (value > bestValue) {
                bestValue = value;
                best = index;
            }
        });
        return best;
    }

    function minIndex(list, score) {
        var best = -1;
        var bestValue = Infinity;
        list.forEach(function (item, index) {
            var value = num(score(item));
            if (value < bestValue) {
                bestValue = value;
                best = index;
            }
        });
        return best;
    }

    /* ----------------------------- 智能推荐 ----------------------------- */

    function loadRecommendOptions() {
        fetchJson('/api/recommend/options').then(function (options) {
            state.recommendOptions = options;
            dom.sceneChips.innerHTML = options.scenes.map(function (item) {
                return '<button type="button" class="chip" data-code="' + escapeHtml(item.code) + '">'
                    + escapeHtml(item.label) + '</button>';
            }).join('');
            dom.energyPreference.innerHTML = options.energyPreferences.map(function (item) {
                return '<option value="' + escapeHtml(item.code) + '">' + escapeHtml(item.label) + '</option>';
            }).join('');
            dom.prioritySelect.innerHTML = options.priorities.map(function (item) {
                return '<option value="' + escapeHtml(item.code) + '">' + escapeHtml(item.label) + '</option>';
            }).join('');
            dom.budgetPresets.innerHTML = options.budgetPresets.map(function (item) {
                return '<button type="button" data-min="' + item.minPrice + '" data-max="' + item.maxPrice + '">'
                    + escapeHtml(item.label) + '</button>';
            }).join('');
            dom.minSeatsSelect.innerHTML = [4, 5, 6, 7].map(function (seat) {
                return '<option value="' + seat + '">' + seat + ' 座</option>';
            }).join('');
            dom.limitSelect.innerHTML = [3, 5, 8, 10].map(function (limit) {
                return '<option value="' + limit + '">' + limit + ' 款</option>';
            }).join('');
            dom.minSeatsSelect.value = '5';
            dom.limitSelect.value = '5';
        }).catch(function (error) {
            toast(error.message);
        });
    }

    function runRecommend() {
        var scenes = Array.prototype.filter.call(dom.sceneChips.querySelectorAll('.chip.is-active'), function (chip) {
            return true;
        }).map(function (chip) {
            return chip.dataset.code;
        });

        var payload = {
            minPrice: Number(dom.recMinPrice.value || 0),
            maxPrice: Number(dom.recMaxPrice.value || 0),
            scenes: scenes,
            energyPreference: dom.energyPreference.value,
            minSeats: Number(dom.minSeatsSelect.value),
            priority: dom.prioritySelect.value,
            limit: Number(dom.limitSelect.value)
        };

        dom.recommendResult.innerHTML = '<p class="placeholder">正在计算推荐结果…</p>';
        fetchJson('/api/recommend', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        }).then(renderRecommendResult).catch(function (error) {
            dom.recommendResult.innerHTML = '<p class="placeholder">推荐失败：' + escapeHtml(error.message) + '</p>';
        });
    }

    function renderRecommendResult(result) {
        if (!result.items || !result.items.length) {
            dom.recommendResult.innerHTML = '<p class="placeholder">没有符合条件的候选车型，请调整预算或需求。</p>';
            return;
        }
        var html = '<div class="recommend-summary">' + escapeHtml(result.summary) + '</div>'
            + '<div class="recommend-list">';

        result.items.forEach(function (item) {
            html += '<div class="rec-item">'
                + '<div class="rec-item__rank">' + item.rank + '</div>'
                + '<div>'
                + '<div class="rec-item__title"><strong>' + escapeHtml(item.car.name) + '</strong>'
                + carThumb(item.car, 'thumb--sm')
                + '<span class="tag">' + num(item.car.price).toFixed(2) + ' 万</span>'
                + '<span class="tag">' + escapeHtml(item.car.energyTypeLabel) + '</span>'
                + '<span class="rec-item__score">' + num(item.score).toFixed(1) + ' 分</span></div>'
                + '<ul class="rec-item__reasons">'
                + item.reasons.map(function (reason) {
                    return '<li>' + escapeHtml(reason) + '</li>';
                }).join('')
                + '</ul>'
                + '<div class="score-bars">'
                + item.details.map(function (detail) {
                    return '<span class="score-bar">' + escapeHtml(detail.label) + ' <b>'
                        + num(detail.weightedScore).toFixed(1) + '</b> / ' + num(detail.weight) + '</span>';
                }).join('')
                + '</div></div></div>';
        });

        html += '</div>';
        dom.recommendResult.innerHTML = html;
    }

    /* ----------------------------- 统计榜单 ----------------------------- */

    function loadStatistics() {
        fetchJson('/api/statistics/overview').then(renderOverview).catch(function (error) {
            toast(error.message);
        });
        fetchJson('/api/statistics/brand-ranking?limit=10').then(renderBrandTable).catch(function (error) {
            toast(error.message);
        });
        fetchJson('/api/statistics/scene-distribution').then(function (items) {
            renderBarList($('#sceneDistribution'), items);
        }).catch(function () {
            /* 忽略局部失败 */
        });
        loadCarRanking(state.rankingScope);
    }

    /** 车型销量榜：支持全部 / 新能源 / 燃油 / 性能车切换 */
    function loadCarRanking(scope) {
        state.rankingScope = scope || 'ALL';
        dom.carRankingBody.innerHTML = '<tr><td colspan="6" class="placeholder">正在加载销量榜…</td></tr>';
        fetchJson('/api/statistics/car-ranking?limit=10&scope=' + encodeURIComponent(state.rankingScope))
            .then(renderCarRanking)
            .catch(function (error) {
                dom.carRankingBody.innerHTML = '<tr><td colspan="6" class="placeholder">加载失败：'
                    + escapeHtml(error.message) + '</td></tr>';
            });
    }

    function renderCarRanking(list) {
        if (!list || !list.length) {
            dom.carRankingBody.innerHTML = '<tr><td colspan="6" class="placeholder">该范围内暂无车型</td></tr>';
            return;
        }
        var maxSales = list.reduce(function (max, item) {
            return Math.max(max, num(item.monthlySales));
        }, 1);

        dom.carRankingBody.innerHTML = list.map(function (item) {
            var thumbCar = {
                id: item.carId,
                brand: item.brand,
                bodyType: item.bodyType,
                bodyTypeLabel: item.bodyTypeLabel
            };
            return '<tr>'
                + '<td><span class="rank-medal' + (item.rank <= 3 ? ' is-top' : '') + '">'
                + item.rank + '</span></td>'
                + '<td><div class="rank-car">' + carThumb(thumbCar, 'thumb--sm')
                + '<span>' + escapeHtml(item.name) + '</span></div></td>'
                + '<td>' + escapeHtml(item.brand) + '</td>'
                + '<td>' + num(item.price).toFixed(2) + ' 万</td>'
                + '<td><div>' + num(item.monthlySales).toLocaleString('zh-CN') + '</div>'
                + '<div class="sales-bar"><span style="width:'
                + Math.round(num(item.monthlySales) / maxSales * 100) + '%"></span></div></td>'
                + '<td>' + (num(item.salesShare) * 100).toFixed(1) + '%</td>'
                + '</tr>';
        }).join('');
    }

    function renderOverview(overview) {
        $('#heroCarCount').textContent = overview.totalCars;
        $('#heroBrandCount').textContent = overview.totalBrands;
        $('#heroStats').innerHTML = [
            {value: overview.totalCars + ' 款', label: '在售车型'},
            {value: overview.totalBrands + ' 个', label: '覆盖品牌'},
            {value: (overview.newEnergyRatio * 100).toFixed(0) + '%', label: '新能源占比'},
            {value: overview.performanceCarCount + ' 款', label: '零百 5 秒内性能车'}
        ].map(function (stat) {
            return '<div class="stat"><div class="stat__value">' + escapeHtml(stat.value) + '</div>'
                + '<div class="stat__label">' + escapeHtml(stat.label) + '</div></div>';
        }).join('');

        renderBarList($('#energyDistribution'), overview.energyDistribution);
        renderBarList($('#priceDistribution'), overview.priceDistribution);
    }

    function renderBarList(container, items) {
        if (!container || !items) {
            return;
        }
        var max = items.reduce(function (acc, item) {
            return Math.max(acc, item.count);
        }, 1);
        container.innerHTML = items.map(function (item) {
            var percent = Math.round((item.count / max) * 100);
            return '<div class="bar-item"><div class="bar-item__head"><span>' + escapeHtml(item.label)
                + '</span><span>' + item.count + ' 款 · ' + (item.ratio * 100).toFixed(1) + '%</span></div>'
                + '<div class="bar-track"><div class="bar-fill" style="width:' + percent + '%"></div></div></div>';
        }).join('');
    }

    function renderBrandTable(rankings) {
        var tbody = dom.brandTableBody || (dom.brandTableBody = $('#brandTable tbody'));
        tbody.innerHTML = rankings.map(function (item) {
            return '<tr>'
                + '<td><span class="rank-medal' + (item.rank <= 3 ? ' is-top' : '') + '">' + item.rank + '</span></td>'
                + '<td><strong>' + escapeHtml(item.brand) + '</strong></td>'
                + '<td>' + item.carCount + '</td>'
                + '<td>' + num(item.totalMonthlySales).toLocaleString('zh-CN') + '</td>'
                + '<td>★ ' + num(item.averageScore).toFixed(2) + '</td>'
                + '<td>' + escapeHtml(item.bestSeller) + '</td>'
                + '</tr>';
        }).join('');
    }

    document.addEventListener('DOMContentLoaded', init);
})();
