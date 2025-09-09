<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<c:set var="PATH_HOME" value="/"/>
<c:set var="PATH_USER" value="/user"/>
<c:set var="PATH_USER_QUESTS" value="/user/quests"/>

<c:set var="PARAM_ID" value="id"/>

<!doctype html>
<html lang="ru">
<head>
    <meta charset="UTF-8"/>
    <title>Профиль пользователя — TextQuest</title>

    <c:url var="cssMain" value="/assets/css/main.css"/>
    <c:url var="cssProfile" value="/assets/css/profile.css"/>
    <link rel="stylesheet" href="${cssMain}"/>
    <link rel="stylesheet" href="${cssProfile}"/>

    <meta name="viewport" content="width=device-width, initial-scale=1"/>
</head>
<body>
<div class="container grid">

    <c:set var="u" value="${empty profileUser ? (empty viewUser ? user : viewUser) : profileUser}"/>
    <c:set var="stats" value="${requestScope.stats}"/>

    <c:url var="homeUrl" value="${PATH_HOME}"/>

    <%@ include file="/WEB-INF/jsp/fragments/alerts.jspf" %>

    <header class="card profile-header">
        <div class="profile-header__text">
            <h1 class="profile-title">
                <c:choose>
                    <c:when test="${not empty u}">
                        Профиль: <c:out value="${u.userName}"/>
                    </c:when>
                    <c:otherwise>Профиль пользователя</c:otherwise>
                </c:choose>
            </h1>
            <p class="muted">Публичная информация и статистика пользователя.</p>
        </div>
        <nav class="actions">
            <a class="btn btn-primary" href="${homeUrl}">На главную</a>
            <c:if test="${not empty u}">
                <c:url var="userQuestsUrl" value="${PATH_USER_QUESTS}">
                    <c:param name="${PARAM_ID}" value="${u.userId}"/>
                </c:url>
                <a class="btn" href="${userQuestsUrl}">Квесты пользователя</a>
            </c:if>
        </nav>
    </header>

    <c:choose>
        <c:when test="${empty u}">
            <section class="card">
                <p class="muted">Пользователь не найден.</p>
            </section>
        </c:when>
        <c:otherwise>
            <section class="card">
                <h2 class="section-title">Информация об аккаунте</h2>
                <div class="kv">
                    <div class="kv-row">
                        <div class="kv-label">ID</div>
                        <div class="kv-value"><code>${u.userId}</code></div>
                    </div>
                    <div class="kv-row">
                        <div class="kv-label">Имя</div>
                        <div class="kv-value"><c:out value="${u.userName}"/></div>
                    </div>
                    <div class="kv-row">
                        <div class="kv-label">Логин</div>
                        <div class="kv-value"><c:out value="${u.userLogin}"/></div>
                    </div>
                    <div class="kv-row">
                        <div class="kv-label">Роль</div>
                        <div class="kv-value"><c:out value="${u.role}"/></div>
                    </div>
                    <div class="kv-row">
                        <div class="kv-label">Создан</div>
                        <div class="kv-value">
                            <fmt:formatDate value="${u.createdAtDate}" pattern="yyyy-MM-dd HH:mm:ss"/>
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
                        <div class="muted">Опубликованные/кастомные квесты</div>
                    </div>
                    <div class="stat">
                        <div class="num"><c:out value="${empty stats ? '—' : stats.questsCompleted}"/></div>
                        <div class="label">Квестов пройдено</div>
                        <div class="muted">Достигнута финальная ветка</div>
                    </div>
                    <div class="stat">
                        <div class="num"><c:out value="${empty stats ? '—' : stats.endingsUnlocked}"/></div>
                        <div class="label">Финальных веток открыто</div>
                        <div class="muted">В главном квесте</div>
                    </div>
                </div>
            </section>
        </c:otherwise>
    </c:choose>

</div>
</body>
</html>