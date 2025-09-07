<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!doctype html>
<html lang="ru">
<head>
    <meta charset="UTF-8"/>
    <title>Уведомления — TextQuest</title>

    <c:url var="cssMain" value="/assets/css/main.css"/>
    <c:url var="cssNotify" value="/assets/css/notifications.css"/>
    <link rel="stylesheet" href="${cssMain}"/>
    <link rel="stylesheet" href="${cssNotify}"/>

    <meta name="viewport" content="width=device-width, initial-scale=1"/>
</head>
<body>
<div class="container grid">

    <c:set var="PATH_HOME" value="/"/>
    <c:set var="PATH_NOTIFICATIONS" value="/notifications"/>
    <c:set var="PARAM_ACTION" value="action"/>
    <c:set var="PARAM_ID" value="id"/>
    <c:set var="ACT_MARK_ALL" value="markAll"/>
    <c:set var="ACT_CLEAR_ALL" value="clearAll"/>
    <c:set var="ACT_MARK_READ" value="markRead"/>
    <c:url var="homeUrl" value="${PATH_HOME}"/>
    <c:url var="notifyUrl" value="${PATH_NOTIFICATIONS}"/>

    <%@ include file="/WEB-INF/jsp/fragments/alerts.jspf" %>

    <header class="card notify-header">
        <div class="notify-header__left">
            <h1 class="notify-title">
                Уведомления
                <c:if test="${unread > 0}">
                    <span class="badge">${unread}</span>
                </c:if>
            </h1>
            <p class="muted">Непрочитанных: <b>${unread}</b></p>
        </div>
        <nav class="actions">
            <a class="btn btn-primary" href="${homeUrl}">На главную</a>
        </nav>
    </header>

    <section class="card">
        <div class="toolbar">
            <form method="post" action="${notifyUrl}">
                <input type="hidden" name="${PARAM_ACTION}" value="${ACT_MARK_ALL}"/>
                <button class="btn primary" type="submit">Пометить всё прочитанным</button>
            </form>
            <form method="post" action="${notifyUrl}">
                <input type="hidden" name="${PARAM_ACTION}" value="${ACT_CLEAR_ALL}"/>
                <button class="btn ghost" type="submit">Очистить всё</button>
            </form>
        </div>
    </section>

    <section class="card">
        <c:choose>
            <c:when test="${empty items}">
                <p class="empty muted">
                    <svg class="icon" viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M12 2a7 7 0 0 1 7 7v3h1a2 2 0 0 1 2 2v6H2v-6a2 2 0 0 1 2-2h1V9a7 7 0 0 1 7-7Z"
                              fill="currentColor"/>
                    </svg>
                    Уведомлений нет.
                </p>
            </c:when>
            <c:otherwise>
                <ul class="notify-list" aria-label="Список уведомлений">
                    <c:forEach var="n" items="${items}">
                        <li class="notify-item${n.read ? '' : ' is-unread'}">
                            <div class="notify-item__main">
                                <div class="notify-item__title">
                                    <c:out value="${n.title}" escapeXml="false"/>
                                    <c:if test="${not n.read}">
                                        <span class="dot" aria-label="Непрочитано"></span>
                                    </c:if>
                                </div>
                                <div class="notify-item__body muted">
                                    <c:out value="${n.body}" escapeXml="false"/>
                                </div>
                            </div>
                            <div class="notify-item__side">
                                <div class="notify-item__date">
                                    <fmt:formatDate value="${n.createdAtDate}" pattern="yyyy-MM-dd HH:mm"/>
                                </div>
                                <form method="post" action="${notifyUrl}">
                                    <input type="hidden" name="${PARAM_ACTION}" value="${ACT_MARK_READ}"/>
                                    <input type="hidden" name="${PARAM_ID}" value="${n.id}"/>
                                    <button class="btn btn-ghost" type="submit" <c:if test="${n.read}">disabled</c:if>>
                                        Прочитано
                                    </button>
                                </form>
                            </div>
                        </li>
                    </c:forEach>
                </ul>
            </c:otherwise>
        </c:choose>
    </section>

</div>
</body>
</html>