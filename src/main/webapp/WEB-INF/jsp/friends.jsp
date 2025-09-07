<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!doctype html>
<html lang="ru">
<head>
    <meta charset="UTF-8"/>
    <title>Друзья — TextQuest</title>

    <c:url var="cssMain" value="/assets/css/main.css"/>
    <c:url var="cssFriends" value="/assets/css/friends.css"/>
    <link rel="stylesheet" href="${cssMain}"/>
    <link rel="stylesheet" href="${cssFriends}"/>

    <meta name="viewport" content="width=device-width, initial-scale=1"/>
</head>
<body>
<div class="container grid">

    <c:set var="PATH_HOME" value="/"/>
    <c:set var="PATH_FRIENDS" value="/friends"/>
    <c:set var="PARAM_ACTION" value="action"/>
    <c:set var="PARAM_ID" value="id"/>
    <c:set var="PARAM_FROM_ID" value="fromId"/>
    <c:set var="ACT_ACCEPT" value="accept"/>
    <c:set var="ACT_DECL" value="decline"/>
    <c:set var="ACT_CANCEL" value="cancel"/>
    <c:set var="ACT_REMOVE" value="remove"/>
    <c:url var="homeUrl" value="${PATH_HOME}"/>
    <c:url var="friendsUrl" value="${PATH_FRIENDS}"/>

    <%@ include file="/WEB-INF/jsp/fragments/alerts.jspf" %>

    <header class="card page-header">
        <div class="page-header__text">
            <h1 class="page-header__title">Друзья</h1>
            <p class="page-header__sub muted">Управляйте заявками и списком друзей.</p>
        </div>
        <nav class="actions">
            <a class="btn btn-primary" href="${homeUrl}">На главную</a>
        </nav>
    </header>

    <c:set var="inCount" value="${empty incoming ? 0 : fn:length(incoming)}"/>
    <c:set var="outCount" value="${empty outgoing ? 0 : fn:length(outgoing)}"/>
    <c:set var="frCount" value="${empty friends  ? 0 : fn:length(friends)}"/>

    <section class="two-col">

        <article class="card">
            <div class="section-header">
                <h2 class="section-title">Входящие заявки</h2>
                <c:if test="${inCount > 0}">
                    <span class="badge">${inCount}</span>
                </c:if>
            </div>

            <c:choose>
                <c:when test="${empty incoming}">
                    <p class="empty muted">
                        <svg class="icon" viewBox="0 0 24 24" aria-hidden="true">
                            <path d="M12 12a5 5 0 1 0-5-5 5 5 0 0 0 5 5Zm0 2c-4.33 0-8 2.17-8 5v1h16v-1c0-2.83-3.67-5-8-5Z"
                                  fill="currentColor"/>
                        </svg>
                        Нет входящих заявок.
                    </p>
                </c:when>
                <c:otherwise>
                    <ul class="list stack" aria-label="Входящие заявки">
                        <c:forEach var="r" items="${incoming}">
                            <li class="item-row">
                                <div class="userbox">
                                    <div class="avatar">?</div>
                                    <div class="userbox__text">
                                        <div class="userbox__title">
                                            От: <code class="code">${r.fromUserId}</code>
                                        </div>
                                        <div class="muted">
                                            <fmt:formatDate value="${r.createdAtDate}" pattern="yyyy-MM-dd HH:mm:ss"/>
                                        </div>
                                    </div>
                                </div>

                                <div class="actions actions--row">
                                    <form method="post" action="${friendsUrl}">
                                        <input type="hidden" name="${PARAM_ACTION}" value="${ACT_ACCEPT}"/>
                                        <input type="hidden" name="${PARAM_FROM_ID}" value="${r.fromUserId}"/>
                                        <button class="btn btn-primary" type="submit">Принять</button>
                                    </form>
                                    <form method="post" action="${friendsUrl}">
                                        <input type="hidden" name="${PARAM_ACTION}" value="${ACT_DECL}"/>
                                        <input type="hidden" name="${PARAM_FROM_ID}" value="${r.fromUserId}"/>
                                        <button class="btn btn-ghost" type="submit">Отклонить</button>
                                    </form>
                                </div>
                            </li>
                        </c:forEach>
                    </ul>
                </c:otherwise>
            </c:choose>
        </article>

        <article class="card">
            <div class="section-header">
                <h2 class="section-title">Исходящие заявки</h2>
                <c:if test="${outCount > 0}">
                    <span class="badge">${outCount}</span>
                </c:if>
            </div>

            <c:choose>
                <c:when test="${empty outgoing}">
                    <p class="empty muted">
                        <svg class="icon" viewBox="0 0 24 24" aria-hidden="true">
                            <path d="M21 7 9 19l-6-6 2-2 4 4 10-10 2 2Z" fill="currentColor"/>
                        </svg>
                        Вы не отправляли заявок.
                    </p>
                </c:when>
                <c:otherwise>
                    <ul class="list stack" aria-label="Исходящие заявки">
                        <c:forEach var="r" items="${outgoing}">
                            <li class="item-row">
                                <div class="userbox">
                                    <div class="avatar">→</div>
                                    <div class="userbox__text">
                                        <div class="userbox__title">
                                            Кому: <code class="code">${r.toUserId}</code>
                                        </div>
                                        <div class="muted">
                                            <fmt:formatDate value="${r.createdAtDate}" pattern="yyyy-MM-dd HH:mm:ss"/>
                                        </div>
                                    </div>
                                </div>

                                <div class="actions actions--row">
                                    <form method="post" action="${friendsUrl}">
                                        <input type="hidden" name="${PARAM_ACTION}" value="${ACT_CANCEL}"/>
                                        <input type="hidden" name="${PARAM_ID}" value="${r.toUserId}"/>
                                        <button class="btn btn-ghost" type="submit">Отменить</button>
                                    </form>
                                </div>
                            </li>
                        </c:forEach>
                    </ul>
                </c:otherwise>
            </c:choose>
        </article>
    </section>

    <section class="card">
        <div class="section-header">
            <h2 class="section-title">Мои друзья</h2>
            <c:if test="${frCount > 0}">
                <span class="badge">${frCount}</span>
            </c:if>
        </div>

        <c:choose>
            <c:when test="${empty friends}">
                <p class="empty muted">
                    <svg class="icon" viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M12 2a7 7 0 0 1 7 7v3h1a2 2 0 0 1 2 2v6H2v-6a2 2 0 0 1 2-2h1V9a7 7 0 0 1 7-7Z"
                              fill="currentColor"/>
                    </svg>
                    Список друзей пуст.
                </p>
            </c:when>
            <c:otherwise>
                <ul class="friend-grid">
                    <c:forEach var="f" items="${friends}">
                        <li class="friend-card">
                            <div class="friend-card__main">
                                <c:set var="nick" value="${empty f.userName ? '?' : fn:substring(f.userName,0,1)}"/>
                                <div class="avatar avatar--lg"><c:out value="${fn:toUpperCase(nick)}"/></div>
                                <div class="friend-card__text">
                                    <div class="friend-name"><c:out value="${f.userName}"/></div>
                                    <div class="muted"><code class="code"><c:out value="${f.userId}"/></code></div>
                                </div>
                            </div>
                            <form method="post" action="${friendsUrl}" class="friend-card__actions">
                                <input type="hidden" name="${PARAM_ACTION}" value="${ACT_REMOVE}"/>
                                <input type="hidden" name="${PARAM_ID}" value="${f.userId}"/>
                                <button class="btn" type="submit">Удалить из друзей</button>
                            </form>
                        </li>
                    </c:forEach>
                </ul>
            </c:otherwise>
        </c:choose>
    </section>

</div>
</body>
</html>
