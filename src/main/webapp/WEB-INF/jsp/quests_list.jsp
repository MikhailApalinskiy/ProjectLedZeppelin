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
<c:set var="isAuth" value="${not empty sessionScope.user}"/>

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
                                            <c:when test="${q.published}">
                                                Опубликован
                                            </c:when>
                                            <c:otherwise>Черновик</c:otherwise>
                                        </c:choose>
                                    </span>
                                </header>

                                <p class="ql-meta">
                                    <span class="ql-meta-item">Автор: <strong><c:out value="${q.ownerLogin}"/></strong></span>
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

                                    <c:if test="${isAuth}">
                                        <c:url var="editUrl" value="${PATH_EDIT}">
                                            <c:param name="load" value="${q.id}"/>
                                        </c:url>
                                        <a class="btn" href="${editUrl}">Редактировать</a>

                                        <form method="post" action="${deleteUrl}" class="ql-delform">
                                            <input type="hidden" name="id" value="${q.id}"/>
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