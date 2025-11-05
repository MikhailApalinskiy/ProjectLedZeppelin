<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="DATE_FMT" value="dd.MM.yyyy HH:mm"/>

<c:set var="PATH_HOME" value="/"/>
<c:set var="PATH_DRAFTS" value="/drafts"/>
<c:set var="PATH_CREATE" value="/create_quest"/>

<c:url var="cssMain" value="/assets/css/main.css"/>
<c:url var="editorDrafts" value="/assets/css/editor-drafts.css"/>
<c:url var="homeUrl" value="${PATH_HOME}"/>
<c:url var="draftsUrl" value="${PATH_DRAFTS}"/>
<c:url var="editorUrl" value="${PATH_CREATE}"/>

<!doctype html>
<html lang="ru">
<head>
    <meta charset="UTF-8"/>
    <title>Мои черновики — TextQuest</title>
    <link rel="stylesheet" href="${cssMain}"/>
    <link rel="stylesheet" href="${editorDrafts}"/>
</head>
<body class="page-drafts">

<main class="container fade-in" role="main">
    <article class="card" aria-labelledby="pageTitle">
        <header class="card-header header-inline-space">
            <h1 id="pageTitle">Мои черновики</h1>

            <nav class="header-actions" aria-label="Действия">
                <a class="btn btn-ghost" href="${homeUrl}">На главную</a>

                <form method="get" action="${draftsUrl}" class="inline-form" style="gap:8px" role="search" aria-label="Поиск черновиков">
                    <label class="sr-only" for="q">Поиск по названию</label>
                    <input id="q"
                           class="input input-name"
                           type="text"
                           name="q"
                           placeholder="Поиск по названию"
                           value="${fn:escapeXml(empty requestScope.q ? param.q : requestScope.q)}"
                           maxlength="120"
                           autocomplete="off"/>
                    <button class="btn" type="submit">Искать</button>
                    <c:if test="${not empty param.q}">
                        <a class="btn btn-ghost" href="${draftsUrl}">Сбросить</a>
                    </c:if>
                </form>

                <form method="post" action="${draftsUrl}" class="inline-form">
                    <input type="hidden" name="action" value="new"/>
                    <label class="sr-only" for="new-draft-name">Название нового черновика</label>
                    <input id="new-draft-name"
                           class="input input-name"
                           type="text"
                           name="name"
                           placeholder="Название нового черновика"
                           minlength="1"
                           maxlength="120"
                           autocomplete="off"/>
                    <button class="btn btn-primary" type="submit">Новый черновик</button>
                </form>
            </nav>
        </header>

        <section class="card-body">
            <%@ include file="/WEB-INF/jsp/fragments/alerts.jspf" %>

            <p class="notice">
                Нажмите <strong>Открыть</strong>, чтобы загрузить черновик в редактор.
                Можно переименовать или удалить черновик прямо здесь.
            </p>

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
                            <c:otherwise>Черновиков пока нет.</c:otherwise>
                        </c:choose>
                    </p>
                </c:when>

                <c:otherwise>
                    <div class="list stack" role="list">
                        <c:forEach var="d" items="${items}">
                            <article class="ql-card" role="listitem" aria-labelledby="d-${d.draftId}-title">
                                <header class="ql-card-head">
                                    <h2 id="d-${d.draftId}-title" class="ql-name">
                                        <c:out value="${empty d.name ? 'Без названия' : d.name}"/>
                                    </h2>

                                    <span class="ql-badge">
                                        <c:choose>
                                            <c:when test="${empty d.targetQuestId}">Новый квест</c:when>
                                            <c:otherwise>
                                                Редактирование: <code><c:out value="${d.targetQuestId}"/></code>
                                            </c:otherwise>
                                        </c:choose>
                                    </span>
                                </header>

                                <p class="ql-meta">
                                    <span class="ql-meta-item">
                                        Стартовый узел: <strong>#<c:out value="${d.startId}"/></strong>
                                    </span>
                                    <span class="ql-dot" aria-hidden="true">·</span>
                                    <span class="ql-meta-item">
                                        Обновлён:
                                        <strong><c:out value="${d.updatedAt}"/></strong>
                                    </span>
                                </p>

                                <div class="stack stack-actions">
                                    <form class="inline-form" method="post" action="${draftsUrl}">
                                        <input type="hidden" name="action" value="rename"/>
                                        <input type="hidden" name="draftId" value="${d.draftId}"/>

                                        <label class="sr-only" for="name-${d.draftId}">Название</label>
                                        <input id="name-${d.draftId}"
                                               name="name"
                                               type="text"
                                               class="input input-rename"
                                               placeholder="Название черновика"
                                               value="${fn:escapeXml(empty d.name ? '' : d.name)}"
                                               minlength="1"
                                               maxlength="120"
                                               autocomplete="off"/>
                                        <button class="btn" type="submit">Переименовать</button>
                                    </form>

                                    <div class="actions">
                                        <form class="inline-form" method="post" action="${draftsUrl}">
                                            <input type="hidden" name="action" value="open"/>
                                            <input type="hidden" name="draftId" value="${d.draftId}"/>
                                            <button class="btn btn-primary" type="submit">Открыть</button>
                                        </form>

                                        <form class="inline-form ql-delform" method="post" action="${draftsUrl}">
                                            <input type="hidden" name="action" value="delete"/>
                                            <input type="hidden" name="draftId" value="${d.draftId}"/>
                                            <button class="btn btn-danger" type="submit">Удалить</button>
                                        </form>
                                    </div>
                                </div>
                            </article>
                        </c:forEach>
                    </div>

                    <c:if test="${pages > 1}">
                        <nav class="pagination"
                             aria-label="Навигация по страницам"
                             style="margin-top:16px; display:flex; gap:6px; flex-wrap:wrap; justify-content:center;">

                            <c:choose>
                                <c:when test="${page > 1}">
                                    <c:url var="prevUrl" value="${draftsUrl}">
                                        <c:param name="page" value="${page - 1}"/>
                                        <c:if test="${not empty param.q}">
                                            <c:param name="q" value="${param.q}"/>
                                        </c:if>
                                    </c:url>
                                    <a class="btn btn-ghost" href="${prevUrl}">« Назад</a>
                                </c:when>
                                <c:otherwise>
                                    <span class="btn btn-ghost muted" aria-disabled="true">« Назад</span>
                                </c:otherwise>
                            </c:choose>

                            <c:forEach var="p" begin="1" end="${pages}">
                                <c:choose>
                                    <c:when test="${p == page}">
                                        <span class="btn btn-primary" aria-current="page">${p}</span>
                                    </c:when>
                                    <c:otherwise>
                                        <c:url var="pUrl" value="${draftsUrl}">
                                            <c:param name="page" value="${p}"/>
                                            <c:if test="${not empty param.q}">
                                                <c:param name="q" value="${param.q}"/>
                                            </c:if>
                                        </c:url>
                                        <a class="btn" href="${pUrl}">${p}</a>
                                    </c:otherwise>
                                </c:choose>
                            </c:forEach>

                            <c:choose>
                                <c:when test="${page < pages}">
                                    <c:url var="nextUrl" value="${draftsUrl}">
                                        <c:param name="page" value="${page + 1}"/>
                                        <c:if test="${not empty param.q}">
                                            <c:param name="q" value="${param.q}"/>
                                        </c:if>
                                    </c:url>
                                    <a class="btn btn-ghost" href="${nextUrl}">Вперёд »</a>
                                </c:when>
                                <c:otherwise>
                                    <span class="btn btn-ghost muted" aria-disabled="true">Вперёд »</span>
                                </c:otherwise>
                            </c:choose>

                        </nav>
                    </c:if>

                </c:otherwise>
            </c:choose>
        </section>
    </article>
</main>

</body>
</html>