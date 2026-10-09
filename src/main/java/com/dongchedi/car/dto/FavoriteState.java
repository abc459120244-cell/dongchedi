package com.dongchedi.car.dto;

import java.util.List;

/**
 * 收藏操作后的状态，前端可直接用 ids 覆盖本地收藏集合。
 */
public record FavoriteState(boolean favorited, int total, List<Long> ids) {
}
