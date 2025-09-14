<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<c:set var="PATH_HOME" value="/"/>
<c:set var="PATH_DELETE" value="/quest/delete"/>
<c:set var="PATH_PLAY" value="/quest"/>
<c:set var="PATH_EDIT" value="/create_quest"/>

<c:set var="PATH_CSS_MAIN" value="/assets/css/main.css"/>
<c:set var="PATH_CSS_LIST" value="/assets/css/quests-list.css"/>

<c:set var="KEY_MY" value="my.quests"/>
<c:set var="KEY_ALL" value="all.quests"/>
<c:set var="DATE_FMT" value="dd.MM.yyyy HH:mm"/>

<!doctype html>
<html lang="ru">
<head>
    <meta charset="UTF-8"/>
    <title>Квесты пользователей — TextQuest</title>

    <c:url var="cssMain" value="${PATH_CSS_MAIN}"/>
    <c:url var="cssList" value="${PATH_CSS_LIST}"/>
    <link rel="stylesheet" href="${cssMain}"/>
    <link rel="stylesheet" href="${cssList}"/>

    <c:url var="homeUrl" value="${PATH_HOME}"/>
    <c:url var="deleteUrl" value="${PATH_DELETE}"/>
</head>
<body class="page-quests">

<c:set var="ROLE_ADMIN" value="ADMIN"/>
<c:set var="user" value="${sessionScope.user}"/>
<c:set var="isAuth" value="${not empty user}"/>
<c:set var="isAdmin" value="${isAuth and user.role eq ROLE_ADMIN}"/>

<c:set var="isAllTab" value="${pageTitleKey eq KEY_ALL}"/>
<c:set var="isMyTab" value="${pageTitleKey eq KEY_MY}"/>

<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="actualSelf" value="${empty selfUrl ? (ctx += request.servletPath) : selfUrl}"/>

<c:choose>
    <c:when test="${fn:startsWith(actualSelf, ctx)}">
        <c:set var="nextRel" value="${fn:substring(actualSelf, fn:length(ctx), fn:length(actualSelf))}"/>
    </c:when>
    <c:otherwise>
        <c:set var="nextRel" value="${actualSelf}"/>
    </c:otherwise>
</c:choose>

<main class="container fade-in" role="main">
    <article class="card" aria-labelledby="pageTitle">
        <header class="card-header header-inline-space">
            <h1 id="pageTitle">
                <c:choose>
                    <c:when test="${pageTitleKey eq KEY_MY}">Мои квесты</c:when>
                    <c:when test="${pageTitleKey eq KEY_ALL}">Квесты пользователей</c:when>
                    <c:otherwise>Квесты</c:otherwise>
                </c:choose>
            </h1>
            <nav class="header-actions" aria-label="Действия">
                <a class="btn btn-ghost" href="${homeUrl}">На главную</a>
            </nav>
        </header>

        <section class="card-body">
            <%@ include file="/WEB-INF/jsp/fragments/alerts.jspf" %>

            <form method="get"
                  action="${actualSelf}"
                  class="ql-search"
                  role="search"
                  aria-label="Поиск квестов">
                <input type="text"
                       name="q"
                       value="${fn:escapeXml(empty requestScope.q ? param.q : requestScope.q)}"
                       placeholder="Поиск по названию"
                       class="input ql-search__input"
                       autocomplete="off"/>
                <div class="actions">
                    <button type="submit" class="btn btn-primary">Искать</button>
                    <c:if test="${not empty param.q}">
                        <a class="btn btn-ghost" href="${actualSelf}">Сбросить</a>
                    </c:if>
                </div>
            </form>

            <c:if test="${not empty param.q}">
                <p class="notice" style="margin-top:10px">
                    Запрос: «<strong><c:out value="${param.q}"/></strong>»
                </p>
            </c:if>

            <c:choose>
                <c:when test="${empty items}">
                    <p class="notice">
                        <c:choose>
                            <c:when test="${not empty param.q}">Ничего не найдено.</c:when>
                            <c:otherwise>Пока пусто.</c:otherwise>
                        </c:choose>
                    </p>
                </c:when>
                <c:otherwise>
                    <div class="ql-grid" role="list">
                        <c:forEach var="qst" items="${items}">
                            <article class="ql-card" role="listitem" aria-labelledby="q-${qst.id}-title">
                                <header class="ql-card-head">
                                    <h2 id="q-${qst.id}-title" class="ql-name">
                                        <c:out value="${qst.name}"/>
                                    </h2>
                                    <span class="ql-badge <c:out value='${qst.published ? "ok" : "muted"}'/>">
                    <c:choose>
                        <c:when test="${qst.published}">Опубликован</c:when>
                        <c:otherwise>Черновик</c:otherwise>
                    </c:choose>
                  </span>
                                </header>

                                <p class="ql-meta">
                                    <c:set var="__ownerName" value="${ownerNameById[qst.ownerId]}"/>
                                    <span class="ql-meta-item">
                                      Автор: <strong><c:out value="${empty __ownerName ? qst.ownerId : __ownerName}"/></strong>
                                    </span>
                                    <span class="ql-dot" aria-hidden="true">·</span>
                                    <span class="ql-meta-item">Узлов: <strong><c:out
                                            value="${fn:length(qst.nodes)}"/></strong></span>
                                    <span class="ql-dot" aria-hidden="true">·</span>
                                    <span class="ql-meta-item">Стартовый узел: <strong>#<c:out
                                            value="${qst.startId}"/></strong></span>
                                </p>

                                <p class="ql-dates">
                                    <c:if test="${not empty createdMap[qst.id]}">
                    <span class="ql-date">Создан:
                      <fmt:formatDate value="${createdMap[qst.id]}" pattern="${DATE_FMT}"/>
                    </span>
                                    </c:if>
                                    <c:if test="${not empty updatedMap[qst.id]}">
                    <span class="ql-date">Обновлён:
                      <fmt:formatDate value="${updatedMap[qst.id]}" pattern="${DATE_FMT}"/>
                    </span>
                                    </c:if>
                                </p>

                                <div class="ql-row">
                                    <c:url var="playUrl" value="${PATH_PLAY}">
                                        <c:param name="custom" value="${qst.id}"/>
                                    </c:url>
                                    <a class="btn btn-primary" href="${playUrl}">Играть</a>

                                    <c:if test="${(isAllTab and isAdmin) or isMyTab}">
                                        <c:url var="editUrl" value="${PATH_EDIT}">
                                            <c:param name="load" value="${qst.id}"/>
                                        </c:url>
                                        <a class="btn" href="${editUrl}">Редактировать</a>

                                        <form method="post" action="${deleteUrl}" class="ql-delform">
                                            <input type="hidden" name="id" value="${qst.id}"/>
                                            <input type="hidden" name="next" value="${nextRel}"/>
                                            <c:if test="${not empty param.q}">
                                                <input type="hidden" name="q" value="${fn:escapeXml(param.q)}"/>
                                            </c:if>
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