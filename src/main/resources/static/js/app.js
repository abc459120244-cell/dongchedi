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
            sort: 'SALES_DESC',
            page: 1,
            size: 12
        },
        lastPage: null,
        favorites: new Set(loadFavorites()),
        favoriteOnly: false,
        compare: [],
        coverSeed: 0
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

        persistFavorites();
        bindEvents();
        loadFilters();
        loadRecommendOptions();
        loadStatistics();
        loadCars();
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

        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape') {
                closeCompare();
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
        if (group === 'minSeats' || group === 'minScore') {
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
        ['minPrice', 'maxPrice', 'minSeats', 'minRange', 'maxFuel', 'minScore'].forEach(function (key) {
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
        ['minPrice', 'maxPrice', 'minSeats', 'minRange', 'maxFuel', 'minScore'].forEach(function (key) {
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
        if (car.newEnergy) {
            badges += '<span class="badge badge--newenergy">新能源</span>';
        }
        badges += '<span class="badge">' + escapeHtml(car.energyTypeLabel) + '</span>';

        var tags = (car.tags || []).slice(0, 3).map(function (tag) {
            return '<span class="tag">' + escapeHtml(tag) + '</span>';
        }).join('');

        return '<article class="car-card">'
            + '<div class="car-card__cover" style="background:' + brandGradient(car.brand) + '">'
            + '<div class="car-card__badges">' + badges + '</div>'
            + '<div class="car-card__brand">' + escapeHtml(car.brand) + ' · ' + escapeHtml(car.bodyTypeLabel) + '</div>'
            + '<div class="car-card__name">' + escapeHtml(car.name) + '</div>'
            + '</div>'
            + '<div class="car-card__body">'
            + '<div class="car-card__price"><strong>' + num(car.price).toFixed(2) + ' 万</strong>'
            + '<small>' + escapeHtml(car.levelLabel) + ' · ' + car.seats + ' 座</small></div>'
            + '<div class="car-card__meta">' + tags + '</div>'
            + '<p class="car-card__desc">' + escapeHtml(car.highlight) + '</p>'
            + '<div class="car-card__meta"><span class="tag">' + escapeHtml(car.energyDescription) + '</span>'
            + '<span class="tag">轴距 ' + car.wheelbaseMm + 'mm</span>'
            + '<span class="tag">' + car.maxPowerKw + 'kW</span></div>'
            + '<div class="car-card__foot">'
            + '<span class="car-card__score"><b>★ ' + num(car.userScore).toFixed(1) + '</b> · 月销 '
            + num(car.monthlySales).toLocaleString('zh-CN') + '</span>'
            + '<span class="card-actions">'
            + '<button type="button" class="icon-btn' + (favorite ? ' is-active' : '')
            + '" data-action="favorite" data-id="' + car.id + '">' + (favorite ? '已收藏' : '收藏') + '</button>'
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
        if (state.favorites.has(id)) {
            state.favorites.delete(id);
        } else {
            state.favorites.add(id);
        }
        persistFavorites();
        renderCars(state.lastPage);
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
        fetchJson('/api/cars/compare?ids=' + encodeURIComponent(ids)).then(function (cars) {
            dom.compareContent.innerHTML = renderCompareTable(cars);
            dom.compareModal.hidden = false;
        }).catch(function (error) {
            toast(error.message);
        });
    }

    function closeCompare() {
        dom.compareModal.hidden = true;
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
    }

    function renderOverview(overview) {
        $('#heroCarCount').textContent = overview.totalCars;
        $('#heroBrandCount').textContent = overview.totalBrands;
        $('#heroStats').innerHTML = [
            {value: overview.totalCars + ' 款', label: '在售车型'},
            {value: overview.totalBrands + ' 个', label: '覆盖品牌'},
            {value: (overview.newEnergyRatio * 100).toFixed(0) + '%', label: '新能源占比'},
            {value: overview.averagePrice.toFixed(1) + ' 万', label: '平均指导价'}
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
