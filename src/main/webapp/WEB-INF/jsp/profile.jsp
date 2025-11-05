<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!doctype html>
<html lang="ru">
<head>
    <meta charset="UTF-8"/>
    <title>Профиль — TextQuest</title>

    <c:url var="cssMain" value="/assets/css/main.css"/>
    <c:url var="cssProfile" value="/assets/css/profile.css"/>
    <link rel="stylesheet" href="${cssMain}"/>
    <link rel="stylesheet" href="${cssProfile}"/>

    <meta name="viewport" content="width=device-width, initial-scale=1"/>
</head>
<body>
<div class="container grid">

    <c:set var="PATH_HOME" value="/"/>
    <c:set var="PATH_PROFILE" value="/profile"/>
    <c:set var="ACT_UPDATE_NAME" value="updateName"/>
    <c:set var="ACT_CHANGE_PASSWORD" value="changePassword"/>
    <c:set var="user" value="${empty requestScope.user ? sessionScope.user : requestScope.user}"/>

    <c:url var="homeUrl" value="${PATH_HOME}"/>
    <c:url var="profileUrl" value="${PATH_PROFILE}"/>

    <%@ include file="/WEB-INF/jsp/fragments/alerts.jspf" %>

    <header class="card profile-header">
        <div class="profile-header__text">
            <h1 class="profile-title">Профиль</h1>
            <p class="muted">Управляйте данными аккаунта и смотрите прогресс по квестам.</p>
        </div>
        <nav class="actions">
            <button type="button" class="btn btn-primary" onclick="location.href='${homeUrl}'">
                На главную
            </button>
        </nav>
    </header>

    <section class="card">
        <h2 class="section-title">Информация об аккаунте</h2>
        <div class="kv">
            <div class="kv-row">
                <div class="kv-label">ID пользователя</div>
                <div class="kv-value"><code>${user.userId}</code></div>
            </div>
            <div class="kv-row">
                <div class="kv-label">Отображаемое имя</div>
                <div class="kv-value"><c:out value="${user.userName}"/></div>
            </div>
            <div class="kv-row">
                <div class="kv-label">Логин</div>
                <div class="kv-value"><c:out value="${user.userLogin}"/></div>
            </div>
            <div class="kv-row">
                <div class="kv-label">Роль</div>
                <div class="kv-value"><c:out value="${user.role}"/></div>
            </div>
            <div class="kv-row">
                <div class="kv-label">Создан</div>
                <div class="kv-value">
                    <fmt:formatDate value="${user.createdAtDate}" pattern="yyyy-MM-dd HH:mm:ss"/>
                </div>
            </div>
        </div>
    </section>

    <section class="card">
        <h2 class="section-title">Статистика</h2>
        <div class="stats two-col">
            <div class="stat">
                <div class="num"><c:out value="${empty stats ? '—' : stats.questsCreated}"/></div>
                <div class="label">Квестов создано</div>
                <div class="muted">Ваши опубликованные/кастомные квесты</div>
            </div>
            <div class="stat">
                <div class="num"><c:out value="${empty stats ? '—' : stats.questsCompleted}"/></div>
                <div class="label">Квестов пройдено</div>
                <div class="muted">Достигнута финальная ветка хотя бы раз</div>
            </div>
            <div class="stat">
                <div class="num"><c:out value="${empty stats ? '—' : stats.endingsUnlocked}"/></div>
                <div class="label">Финальных веток открыто</div>
                <div class="muted">В главном квесте</div>
            </div>
        </div>
    </section>

    <div class="two-col">

        <section class="card">
            <h3 class="section-subtitle">Изменить отображаемое имя</h3>
            <c:url var="updateNameUrl" value="${profileUrl}">
                <c:param name="action" value="${ACT_UPDATE_NAME}"/>
            </c:url>

            <form method="post" action="${updateNameUrl}" class="form" novalidate>
                <div class="field">
                    <label for="displayName" class="label">Новое имя</label>
                    <div class="input-wrap">
                        <svg class="icon-left" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
                            <path d="M12 12a5 5 0 1 0-5-5 5 5 0 0 0 5 5Zm0 2c-4.33 0-8 2.17-8 5v1h16v-1c0-2.83-3.67-5-8-5Z"
                                  fill="currentColor"/>
                        </svg>
                        <input id="displayName" name="displayName" class="input"
                               type="text" minlength="2" maxlength="60" required
                               placeholder="Например: Михаил" autocomplete="name"/>
                    </div>
                    <div class="help">До 20 символов. Видно другим пользователям.</div>
                </div>

                <div class="actions">
                    <button class="btn primary" type="submit">Сохранить</button>
                    <button class="btn ghost" type="reset">Сбросить</button>
                </div>
            </form>
        </section>

        <section class="card">
            <h3 class="section-subtitle">Сменить пароль</h3>
            <c:url var="changePwdUrl" value="${profileUrl}">
                <c:param name="action" value="${ACT_CHANGE_PASSWORD}"/>
            </c:url>

            <form method="post" action="${changePwdUrl}" class="form" autocomplete="off" novalidate>
                <div class="field">
                    <label for="currentPassword" class="label">Текущий пароль</label>
                    <div class="input-wrap">
                        <svg class="icon-left" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
                            <path d="M17 8V7a5 5 0 0 0-10 0v1H5v12h14V8Zm-8 0V7a3 3 0 0 1 6 0v1Z" fill="currentColor"/>
                        </svg>
                        <input id="currentPassword" name="currentPassword" class="input" type="password"
                               autocomplete="current-password" required/>
                    </div>
                </div>

                <div class="field">
                    <label for="newPassword" class="label">Новый пароль</label>
                    <div class="input-wrap">
                        <svg class="icon-left" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
                            <path d="M12 2a7 7 0 0 1 7 7v3h1a2 2 0 0 1 2 2v6H2v-6a2 2 0 0 1 2-2h1V9a7 7 0 0 1 7-7Z"
                                  fill="currentColor"/>
                        </svg>
                        <input id="newPassword" name="newPassword" class="input" type="password"
                               minlength="6" autocomplete="new-password" required/>
                    </div>
                    <div class="help">Минимум 6 символов.</div>
                </div>

                <div class="field">
                    <label for="confirmPassword" class="label">Повторите новый пароль</label>
                    <div class="input-wrap">
                        <svg class="icon-left" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
                            <path d="M21 7 9 19l-6-6 2-2 4 4 10-10 2 2Z" fill="currentColor"/>
                        </svg>
                        <input id="confirmPassword" name="confirmPassword" class="input" type="password"
                               minlength="6" autocomplete="new-password" required/>
                    </div>
                </div>

                <div class="actions">
                    <button class="btn primary" type="submit">Обновить пароль</button>
                    <button class="btn ghost" type="reset">Сбросить</button>
                </div>
            </form>
        </section>

    </div>
</div>
</body>
</html>