<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<c:set var="PATH_HOME" value="/"/>
<c:set var="PATH_PLAY" value="/quest"/>
<c:set var="PATH_EDIT" value="/create_quest"/>
<c:set var="PATH_DELETE" value="/quest/delete"/>
<c:set var="PATH_USER" value="/user"/>

<c:set var="KEY_TITLE" value="user.quests"/>
<c:set var="DATE_FMT" value="dd.MM.yyyy HH:mm"/>

<c:set var="PARAM_ID" value="id"/>

<!doctype html>
<html lang="ru">
<head>
    <meta charset="UTF-8"/>
    <title>Квесты пользователя — TextQuest</title>

    <c:url var="cssMain" value="/assets/css/main.css"/>
    <c:url var="cssList" value="/assets/css/quests-list.css"/>
    <link rel="stylesheet" href="${cssMain}"/>
    <link rel="stylesheet" href="${cssList}"/>

    <c:url var="homeUrl" value="${PATH_HOME}"/>
    <c:url var="deleteUrl" value="${PATH_DELETE}"/>
</head>
<body class="page-quests">

<c:set var="u" value="${empty viewUser ? user : viewUser}"/>

<c:set var="ROLE_ADMIN" value="ADMIN"/>
<c:set var="me" value="${sessionScope.user}"/>
<c:set var="isAuth" value="${not empty me}"/>
<c:set var="isAdmin" value="${isAuth and me.role eq ROLE_ADMIN}"/>
<c:set var="isOwner" value="${isAuth and not empty u and me.userId eq u.userId}"/>
<c:set var="selfUrl"
       value="${pageContext.request.contextPath}${pageContext.request.servletPath}?${PARAM_ID}=${u.userId}"/>

<main class="container fade-in" role="main">
    <article class="card" aria-labelledby="pageTitle">
        <header class="card-header header-inline-space">
            <h1 id="pageTitle">
                <c:choose>
                    <c:when test="${not empty u}">
                        Квесты пользователя: <c:out value="${u.userName}"/>
                        <span class="muted">(@<c:out value="${u.userLogin}"/>)</span>
                    </c:when>
                    <c:otherwise>Квесты пользователя</c:otherwise>
                </c:choose>
            </h1>
            <nav class="header-actions" aria-label="Действия">
                <a class="btn btn-ghost" href="${homeUrl}">На главную</a>
                <c:if test="${not empty u}">
                    <c:url var="userProfileUrl" value="${PATH_USER}">
                        <c:param name="${PARAM_ID}" value="${u.userId}"/>
                    </c:url>
                    <a class="btn" href="${userProfileUrl}">Профиль пользователя</a>
                </c:if>
            </nav>
        </header>

        <section class="card-body">
            <%@ include file="/WEB-INF/jsp/fragments/alerts.jspf" %>

            <c:choose>
                <c:when test="${empty items}">
                    <p class="notice">Пока пусто.</p>
                </c:when>
                <c:otherwise>
                    <div class="ql-grid" role="list">
                        <c:forEach var="q" items="${items}">
                            <article class="ql-card" role="listitem" aria-labelledby="q-${q.id}-title">
                                <header class="ql-card-head">
                                    <h2 id="q-${q.id}-title" class="ql-name">
                                        <c:out value="${q.name}"/>
                                    </h2>
                                    <span class="ql-badge <c:out value='${q.published ? "ok" : "muted"}'/>">
                                        <c:choose>
                                            <c:when test="${q.published}">Опубликован</c:when>
                                            <c:otherwise>Черновик</c:otherwise>
                                        </c:choose>
                                    </span>
                                </header>

                                <p class="ql-meta">
                                    <c:set var="__ownerName" value="${ownerNameById[q.ownerId]}"/>
                                    <span class="ql-meta-item">
                                        Автор: <strong><c:out value="${empty __ownerName ? q.ownerId : __ownerName}"/></strong>
                                    </span>
                                    <span class="ql-dot" aria-hidden="true">·</span>
                                    <span class="ql-meta-item">Узлов: <strong><c:out
                                            value="${fn:length(q.nodes)}"/></strong></span>
                                    <span class="ql-dot" aria-hidden="true">·</span>
                                    <span class="ql-meta-item">Стартовый узел: <strong>#<c:out
                                            value="${q.startId}"/></strong></span>
                                </p>

                                <p class="ql-dates">
                                    <c:if test="${not empty createdMap[q.id]}">
                                        <span class="ql-date">
                                            Создан: <fmt:formatDate value="${createdMap[q.id]}" pattern="${DATE_FMT}"/>
                                        </span>
                                    </c:if>
                                    <c:if test="${not empty updatedMap[q.id]}">
                                        <span class="ql-date">
                                            Обновлён: <fmt:formatDate value="${updatedMap[q.id]}"
                                                                      pattern="${DATE_FMT}"/>
                                        </span>
                                    </c:if>
                                </p>

                                <div class="ql-row">
                                    <c:url var="playUrl" value="${PATH_PLAY}">
                                        <c:param name="custom" value="${q.id}"/>
                                    </c:url>
                                    <a class="btn btn-primary" href="${playUrl}">Играть</a>

                                    <c:if test="${isOwner or isAdmin}">
                                        <c:url var="editUrl" value="${PATH_EDIT}">
                                            <c:param name="load" value="${q.id}"/>
                                        </c:url>
                                        <a class="btn" href="${editUrl}">Редактировать</a>

                                        <form method="post" action="${deleteUrl}" class="ql-delform">
                                            <input type="hidden" name="id" value="${q.id}"/>
                                            <input type="hidden" name="next" value="${selfUrl}"/>
                                            <button type="submit" class="btn btn-danger">Удалить</button>
                                        </form>
                                    </c:if>
                                </div>
                            </article>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>
    </article>
</main>

</body>
</html>