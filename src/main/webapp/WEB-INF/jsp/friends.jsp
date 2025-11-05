<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<c:set var="PATH_HOME" value="/"/>
<c:set var="PATH_FRIENDS" value="/friends"/>
<c:set var="PATH_USER_PUBLIC" value="/user"/>
<c:set var="PATH_USER_QUESTS" value="/user/quests"/>

<c:set var="PARAM_ACTION" value="action"/>
<c:set var="PARAM_ID" value="id"/>
<c:set var="PARAM_FROM_ID" value="fromId"/>
<c:set var="ACT_ACCEPT" value="accept"/>
<c:set var="ACT_DECL" value="decline"/>
<c:set var="ACT_CANCEL" value="cancel"/>
<c:set var="ACT_REMOVE" value="remove"/>

<c:set var="page" value="${empty page  ? 1  : page}"/>
<c:set var="size" value="${empty size  ? 10 : size}"/>
<c:set var="total" value="${empty total ? 0  : total}"/>
<c:set var="pages" value="${empty pages ? 1  : pages}"/>
<c:set var="selfUrl" value="${empty selfUrl ? PATH_FRIENDS : selfUrl}"/>

<c:set var="q" value="${empty param.q ? requestScope.q : param.q}"/>

<!doctype html>
<html lang="ru">
<head>
    <meta charset="UTF-8"/>
    <title>Друзья — TextQuest</title>

    <c:url var="cssMain" value="/assets/css/main.css"/>
    <c:url var="cssFriends" value="/assets/css/friends.css"/>
    <link rel="stylesheet" href="${cssMain}"/>
    <link rel="stylesheet" href="${cssFriends}"/>
</head>
<body class="page-friends">
<main class="container fade-in" role="main">

    <%@ include file="/WEB-INF/jsp/fragments/alerts.jspf" %>

    <article class="card" aria-labelledby="pageTitle">
        <header class="card-header header-inline-space">
            <h1 id="pageTitle">Друзья</h1>
            <nav class="header-actions" aria-label="Действия">
                <c:url var="homeUrl" value="${PATH_HOME}"/>
                <a class="btn btn-ghost" href="${homeUrl}">На главную</a>
            </nav>
        </header>

        <section class="card-body">

            <section class="card subcard">
                <header class="card-header">
                    <h2>Входящие заявки</h2>
                    <c:set var="inCount" value="${empty incoming ? 0 : fn:length(incoming)}"/>
                    <c:if test="${inCount > 0}">
                        <span class="badge">${inCount}</span>
                    </c:if>
                </header>

                <c:choose>
                    <c:when test="${empty incoming}">
                        <p class="notice muted">Нет входящих заявок.</p>
                    </c:when>
                    <c:otherwise>
                        <ul class="list stack" aria-label="Входящие заявки">
                            <c:forEach var="r" items="${incoming}">
                                <li class="item-row">
                                    <div class="userbox">
                                        <div class="avatar">?</div>
                                        <div class="userbox__text">
                                            <div class="userbox__title">
                                                От: <code class="code">${r.fromUser.userId}</code>
                                            </div>
                                            <div class="muted">
                                                <fmt:formatDate value="${r.createdAtDate}"
                                                                pattern="yyyy-MM-dd HH:mm:ss"/>
                                            </div>
                                        </div>
                                    </div>
                                    <div class="actions actions--row">
                                        <form method="post" action="${selfUrl}">
                                            <input type="hidden" name="${PARAM_ACTION}" value="${ACT_ACCEPT}"/>
                                            <input type="hidden" name="${PARAM_FROM_ID}" value="${r.fromUser.userId}"/>
                                            <button class="btn btn-primary" type="submit">Принять</button>
                                        </form>
                                        <form method="post" action="${selfUrl}">
                                            <input type="hidden" name="${PARAM_ACTION}" value="${ACT_DECL}"/>
                                            <input type="hidden" name="${PARAM_FROM_ID}" value="${r.fromUser.userId}"/>
                                            <button class="btn btn-ghost" type="submit">Отклонить</button>
                                        </form>
                                    </div>
                                </li>
                            </c:forEach>
                        </ul>
                    </c:otherwise>
                </c:choose>
            </section>

            <section class="card subcard">
                <header class="card-header">
                    <h2>Исходящие заявки</h2>
                    <c:set var="outCount" value="${empty outgoing ? 0 : fn:length(outgoing)}"/>
                    <c:if test="${outCount > 0}">
                        <span class="badge">${outCount}</span>
                    </c:if>
                </header>

                <c:choose>
                    <c:when test="${empty outgoing}">
                        <p class="notice muted">Вы не отправляли заявок.</p>
                    </c:when>
                    <c:otherwise>
                        <ul class="list stack" aria-label="Исходящие заявки">
                            <c:forEach var="r" items="${outgoing}">
                                <li class="item-row">
                                    <div class="userbox">
                                        <div class="avatar">→</div>
                                        <div class="userbox__text">
                                            <div class="userbox__title">
                                                Кому: <code class="code">${r.toUser.userId}</code>
                                            </div>
                                            <div class="muted">
                                                <fmt:formatDate value="${r.createdAtDate}"
                                                                pattern="yyyy-MM-dd HH:mm:ss"/>
                                            </div>
                                        </div>
                                    </div>
                                    <div class="actions actions--row">
                                        <form method="post" action="${selfUrl}">
                                            <input type="hidden" name="${PARAM_ACTION}" value="${ACT_CANCEL}"/>
                                            <input type="hidden" name="${PARAM_ID}" value="${r.toUser.userId}"/>
                                            <button class="btn btn-ghost" type="submit">Отменить</button>
                                        </form>
                                    </div>
                                </li>
                            </c:forEach>
                        </ul>
                    </c:otherwise>
                </c:choose>
            </section>

            <section class="card subcard">
                <header class="card-header">
                    <h2>Мои друзья</h2>
                    <c:if test="${total > 0}">
                        <span class="badge">${total}</span>
                    </c:if>
                </header>

                <form method="get"
                      action="${selfUrl}"
                      class="search-form"
                      role="search"
                      aria-label="Поиск друзей">
                    <input type="text"
                           name="q"
                           value="${fn:escapeXml(param.q)}"
                           placeholder="Поиск по имени, логину или id"
                           class="input"
                           autocomplete="off"/>
                    <input type="hidden" name="size" value="${size}"/>
                    <button type="submit" class="btn btn-primary">Искать</button>
                    <c:if test="${not empty param.q}">
                        <a class="btn btn-ghost" href="${selfUrl}">Сбросить</a>
                    </c:if>
                </form>

                <c:if test="${not empty q}">
                    <p class="notice" style="margin-top:10px">
                        Запрос: «<strong><c:out value="${q}"/></strong>»
                    </p>
                </c:if>

                <c:choose>
                    <c:when test="${empty friends}">
                        <p class="notice muted">
                            <c:choose>
                                <c:when test="${not empty q}">Ничего не найдено.</c:when>
                                <c:otherwise>Список друзей пуст.</c:otherwise>
                            </c:choose>
                        </p>
                    </c:when>
                    <c:otherwise>
                        <div class="friend-grid" role="list">
                            <c:forEach var="f" items="${friends}">
                                <article class="friend-card" role="listitem">
                                    <div class="friend-card__main">
                                        <c:set var="nick"
                                               value="${empty f.userName ? '?' : fn:substring(f.userName,0,1)}"/>
                                        <div class="avatar avatar--lg"><c:out value="${fn:toUpperCase(nick)}"/></div>
                                        <div class="friend-card__text">
                                            <div class="friend-name"><c:out value="${f.userName}"/></div>
                                            <div class="muted">
                                                <code class="code"><c:out value="${f.userId}"/></code>
                                            </div>
                                        </div>
                                    </div>

                                    <div class="friend-card__actions">
                                        <c:url var="friendProfileUrl" value="${PATH_USER_PUBLIC}">
                                            <c:param name="id" value="${f.userId}"/>
                                        </c:url>
                                        <a class="btn btn-primary" href="${friendProfileUrl}">Профиль</a>

                                        <c:url var="friendQuestsUrl" value="${PATH_USER_QUESTS}">
                                            <c:param name="id" value="${f.userId}"/>
                                        </c:url>
                                        <a class="btn" href="${friendQuestsUrl}">Квесты</a>

                                        <form method="post" action="${selfUrl}">
                                            <input type="hidden" name="${PARAM_ACTION}" value="${ACT_REMOVE}"/>
                                            <input type="hidden" name="${PARAM_ID}" value="${f.userId}"/>
                                            <button class="btn btn-ghost" type="submit">Удалить</button>
                                        </form>
                                    </div>
                                </article>
                            </c:forEach>
                        </div>

                        <c:if test="${pages > 1}">
                            <nav class="pagination"
                                 aria-label="Навигация по друзьям"
                                 style="margin-top:16px; display:flex; gap:6px; flex-wrap:wrap; justify-content:center;">

                                <c:choose>
                                    <c:when test="${page > 1}">
                                        <c:url var="prevUrl" value="${selfUrl}">
                                            <c:param name="page" value="${page - 1}"/>
                                            <c:param name="size" value="${size}"/>
                                            <c:if test="${not empty q}">
                                                <c:param name="q" value="${q}"/>
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
                                            <c:url var="pUrl" value="${selfUrl}">
                                                <c:param name="page" value="${p}"/>
                                                <c:param name="size" value="${size}"/>
                                                <c:if test="${not empty q}">
                                                    <c:param name="q" value="${q}"/>
                                                </c:if>
                                            </c:url>
                                            <a class="btn" href="${pUrl}">${p}</a>
                                        </c:otherwise>
                                    </c:choose>
                                </c:forEach>

                                <c:choose>
                                    <c:when test="${page < pages}">
                                        <c:url var="nextUrl" value="${selfUrl}">
                                            <c:param name="page" value="${page + 1}"/>
                                            <c:param name="size" value="${size}"/>
                                            <c:if test="${not empty q}">
                                                <c:param name="q" value="${q}"/>
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

        </section>
    </article>
</main>
</body>
</html>