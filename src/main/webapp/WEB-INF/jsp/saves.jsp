<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="PATH_HOME" value="/"/>
<c:set var="PATH_SAVES" value="/saves"/>
<c:set var="PATH_QUEST" value="/quest"/>

<c:set var="PATH_CSS_MAIN" value="/assets/css/main.css"/>
<c:set var="PATH_CSS_SAVES" value="/assets/css/saves.css"/>

<c:set var="NEXT_VALUE" value="${param.next}"/>
<c:set var="PURPOSE_VALUE" value="${param.purpose}"/>
<c:set var="NODE_VALUE" value="${param.node}"/>
<c:set var="CUSTOM_VALUE" value="${param.custom}"/>
<c:set var="HAS_NODE" value="${not empty NODE_VALUE}"/>

<c:set var="CONTINUE_BASE" value="${empty NEXT_VALUE ? PATH_QUEST : NEXT_VALUE}"/>

<!doctype html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <title>Слоты сохранений — TextQuest</title>

    <c:url var="cssMain" value="${PATH_CSS_MAIN}"/>
    <c:url var="cssSaves" value="${PATH_CSS_SAVES}"/>
    <link rel="stylesheet" href="${cssMain}">
    <link rel="stylesheet" href="${cssSaves}">
</head>
<body class="page-loads">

<c:url var="homeUrl" value="${PATH_HOME}"/>
<c:url var="savesAction" value="${PATH_SAVES}"/>

<c:url var="continueUrl" value="${CONTINUE_BASE}">
    <c:param name="id" value="${NODE_VALUE}"/>
    <c:if test="${not empty CUSTOM_VALUE}">
        <c:param name="custom" value="${CUSTOM_VALUE}"/>
    </c:if>
</c:url>

<main class="container fade-in" role="main">
    <article class="card" aria-labelledby="title">
        <header class="card-header header-inline-space"
                style="display:flex; justify-content:space-between; align-items:flex-start;">
            <h1 id="title">Слоты сохранений</h1>

            <nav class="header-actions"
                 aria-label="Навигация"
                 style="display:flex; flex-direction:column; gap:8px; align-items:flex-end; min-width:160px;">
                <a class="btn" id="goHome" href="${homeUrl}" style="width:100%; text-align:center;">
                    На главную
                </a>
                <c:if test="${HAS_NODE}">
                    <a class="btn continue" href="${continueUrl}" style="width:100%; text-align:center;">
                        Продолжить
                    </a>
                </c:if>
            </nav>
        </header>

        <section class="card-body">
            <%@ include file="/WEB-INF/jsp/fragments/alerts.jspf" %>

            <p class="notice">Выберите слот, чтобы продолжить, или создайте новое сохранение.</p>
            <c:if test="${HAS_NODE}">
                <p class="notice">Будет сохранён узел #<c:out value="${NODE_VALUE}"/>.</p>
            </c:if>

            <div class="slots">
                <c:choose>
                    <c:when test="${not empty slots}">
                        <c:forEach var="slot" items="${slots}">
                            <c:set var="idx" value="${slot.index}"/>
                            <c:set var="nodeId" value="${slot.nodeId}"/>
                            <c:set var="isEmpty" value="${nodeId == null or nodeId lt 0}"/>
                            <c:set var="questId" value="${empty slot.questId ? 'main' : slot.questId}"/>
                            <c:set var="title" value="${slot.title}"/>
                            <c:set var="updated" value="${slot.updatedAtText}"/>
                            <c:set var="questName"
                                   value="${empty slot.questName
                                           ? (questId eq 'main' ? 'Главный квест' : 'Пользовательский квест')
                                           : slot.questName}"/>

                            <div class="slot-row">
                                <form method="post" action="${savesAction}" style="margin:0">
                                    <input type="hidden" name="op" value="go"/>
                                    <input type="hidden" name="slot" value="${idx}"/>
                                    <input type="hidden" name="next" value="${NEXT_VALUE}"/>
                                    <input type="hidden" name="purpose" value="${PURPOSE_VALUE}"/>
                                    <input type="hidden" name="node" value="${NODE_VALUE}"/>
                                    <c:if test="${not empty CUSTOM_VALUE}">
                                        <input type="hidden" name="custom" value="${CUSTOM_VALUE}"/>
                                    </c:if>

                                    <button type="submit"
                                            class="slot-btn"
                                            id="slot-${idx}"
                                            data-slot="${idx}"
                                            data-node-id="<c:out value='${isEmpty ? "" : nodeId}'/>"
                                            data-quest-id="<c:out value='${questId}'/>"
                                            data-quest-name="<c:out value='${questName}'/>">
                                        <div class="slot-label">
                                            Слот ${idx + 1} —
                                            <c:choose>
                                                <c:when test="${isEmpty}">
                                                    Новое сохранение
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="slot-quest"><c:out value="${questName}"/></span>
                                                    &nbsp;•&nbsp;
                                                    <span class="slot-title">
                                                        <c:out value="${not empty title ? title : ('Узел #' + nodeId)}"/>
                                                    </span>
                                                    <c:if test="${not empty updated}">
                                                        &nbsp;•&nbsp;<span class="slot-updated"><c:out
                                                            value="${updated}"/></span>
                                                    </c:if>
                                                </c:otherwise>
                                            </c:choose>
                                        </div>
                                    </button>
                                </form>

                                <form method="post" action="${savesAction}" style="margin:0">
                                    <input type="hidden" name="op" value="delete"/>
                                    <input type="hidden" name="slot" value="${idx}"/>
                                    <input type="hidden" name="next" value="${NEXT_VALUE}"/>
                                    <input type="hidden" name="purpose" value="${PURPOSE_VALUE}"/>
                                    <input type="hidden" name="node" value="${NODE_VALUE}"/>
                                    <c:if test="${not empty CUSTOM_VALUE}">
                                        <input type="hidden" name="custom" value="${CUSTOM_VALUE}"/>
                                    </c:if>

                                    <button type="submit" class="btn btn-danger slot-delete" id="del-${idx}">
                                        Удалить
                                    </button>
                                </form>
                            </div>
                        </c:forEach>
                    </c:when>

                    <c:otherwise>
                        <c:forEach var="i" begin="0" end="9">
                            <div class="slot-row">
                                <form method="post" action="${savesAction}" style="margin:0">
                                    <input type="hidden" name="op" value="go"/>
                                    <input type="hidden" name="slot" value="${i}"/>
                                    <input type="hidden" name="next" value="${NEXT_VALUE}"/>
                                    <input type="hidden" name="purpose" value="${PURPOSE_VALUE}"/>
                                    <input type="hidden" name="node" value="${NODE_VALUE}"/>
                                    <c:if test="${not empty CUSTOM_VALUE}">
                                        <input type="hidden" name="custom" value="${CUSTOM_VALUE}"/>
                                    </c:if>

                                    <button type="submit"
                                            class="slot-btn"
                                            id="slot-${i}"
                                            data-slot="${i}"
                                            data-node-id=""
                                            data-quest-id="main"
                                            data-quest-name="Главный квест">
                                        <div class="slot-label">Слот ${i + 1} — Новое сохранение</div>
                                    </button>
                                </form>

                                <form method="post" action="${savesAction}" style="margin:0">
                                    <input type="hidden" name="op" value="delete"/>
                                    <input type="hidden" name="slot" value="${i}"/>
                                    <input type="hidden" name="next" value="${NEXT_VALUE}"/>
                                    <input type="hidden" name="purpose" value="${PURPOSE_VALUE}"/>
                                    <input type="hidden" name="node" value="${NODE_VALUE}"/>
                                    <c:if test="${not empty CUSTOM_VALUE}">
                                        <input type="hidden" name="custom" value="${CUSTOM_VALUE}"/>
                                    </c:if>

                                    <button type="submit" class="btn btn-danger slot-delete" id="del-${i}">
                                        Удалить
                                    </button>
                                </form>
                            </div>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </div>
        </section>
    </article>
</main>

</body>
</html>