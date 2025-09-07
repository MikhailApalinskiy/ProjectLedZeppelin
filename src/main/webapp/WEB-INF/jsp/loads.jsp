<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title>Загрузить сохранение — TextQuest</title>

    <c:url var="cssMain" value="/assets/css/main.css"/>
    <c:url var="cssSaves" value="/assets/css/saves.css"/>
    <link rel="stylesheet" href="${cssMain}">
    <link rel="stylesheet" href="${cssSaves}"/>
</head>
<body class="page-loads">

<c:set var="PATH_HOME" value="/"/>
<c:set var="PATH_LOADS" value="/loads"/>
<c:set var="PATH_QUEST" value="/quest"/>

<c:set var="PARAM_OP" value="op"/>
<c:set var="PARAM_SLOT" value="slot"/>
<c:set var="PARAM_NEXT" value="next"/>
<c:set var="PARAM_PURPOSE" value="purpose"/>
<c:set var="PARAM_NODE" value="node"/>
<c:set var="PARAM_CUSTOM" value="custom"/>
<c:set var="PARAM_ID" value="id"/>

<c:set var="OP_GO" value="go"/>
<c:set var="OP_DELETE" value="delete"/>

<c:url var="homeUrl" value="${PATH_HOME}"/>
<c:url var="loadsAction" value="${PATH_LOADS}"/>

<c:url var="continueUrl" value="${empty param[PARAM_NEXT] ? PATH_QUEST : param[PARAM_NEXT]}">
    <c:param name="${PARAM_ID}" value="${param[PARAM_NODE]}"/>
    <c:if test="${not empty param[PARAM_CUSTOM]}">
        <c:param name="${PARAM_CUSTOM}" value="${param[PARAM_CUSTOM]}"/>
    </c:if>
</c:url>

<main class="container fade-in" role="main">
    <article class="card" aria-labelledby="title">

        <header class="card-header header-inline-space"
                style="display:flex; justify-content:space-between; align-items:flex-start;">
            <h1 id="title">Загрузить сохранение</h1>

            <nav class="header-actions"
                 aria-label="Навигация"
                 style="display:flex; flex-direction:column; gap:8px; align-items:flex-end; min-width:160px;">
                <a class="btn" id="goHome" href="${homeUrl}" style="width:100%; text-align:center;">
                    На главную
                </a>
                <c:if test="${not empty param[PARAM_NODE]}">
                    <a class="btn continue" href="${continueUrl}" style="width:100%; text-align:center;">
                        Продолжить
                    </a>
                </c:if>
            </nav>
        </header>

        <section class="card-body">
            <%@ include file="/WEB-INF/jsp/fragments/alerts.jspf" %>

            <p class="notice">Выберите непустой слот, чтобы продолжить приключение.</p>

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
                                <c:choose>
                                    <c:when test="${not isEmpty}">
                                        <form method="post" action="${loadsAction}" style="margin:0">
                                            <input type="hidden" name="${PARAM_OP}" value="${OP_GO}"/>
                                            <input type="hidden" name="${PARAM_SLOT}" value="${idx}"/>
                                            <input type="hidden" name="${PARAM_NEXT}" value="${param[PARAM_NEXT]}"/>
                                            <input type="hidden" name="${PARAM_PURPOSE}"
                                                   value="${param[PARAM_PURPOSE]}"/>
                                            <input type="hidden" name="${PARAM_NODE}" value="${param[PARAM_NODE]}"/>

                                            <button type="submit"
                                                    class="slot-btn"
                                                    id="slot-${idx}"
                                                    data-slot="${idx}"
                                                    data-node-id="${nodeId}"
                                                    data-quest-id="${questId}"
                                                    data-quest-name="<c:out value='${questName}'/>">
                                                <div class="slot-label">
                                                    Слот ${idx + 1} —
                                                    <span class="slot-quest"><c:out value="${questName}"/></span>
                                                    &nbsp;•&nbsp;
                                                    <span class="slot-title">
                                                        <c:out value="${not empty title ? title : ('Узел #' + nodeId)}"/>
                                                    </span>
                                                    <c:if test="${not empty updated}">&nbsp;•&nbsp;
                                                        <span class="slot-updated"><c:out value="${updated}"/></span>
                                                    </c:if>
                                                </div>
                                            </button>
                                        </form>
                                    </c:when>

                                    <c:otherwise>
                                        <button type="button"
                                                class="slot-btn"
                                                id="slot-${idx}"
                                                data-slot="${idx}"
                                                disabled
                                                aria-disabled="true">
                                            <div class="slot-label">Слот ${idx + 1} — пусто</div>
                                        </button>
                                    </c:otherwise>
                                </c:choose>

                                <form method="post" action="${loadsAction}" style="margin:0">
                                    <input type="hidden" name="${PARAM_OP}" value="${OP_DELETE}"/>
                                    <input type="hidden" name="${PARAM_SLOT}" value="${idx}"/>
                                    <input type="hidden" name="${PARAM_NEXT}" value="${param[PARAM_NEXT]}"/>
                                    <input type="hidden" name="${PARAM_PURPOSE}" value="${param[PARAM_PURPOSE]}"/>
                                    <input type="hidden" name="${PARAM_NODE}" value="${param[PARAM_NODE]}"/>
                                    <button type="submit" class="btn btn-danger slot-delete">Удалить</button>
                                </form>
                            </div>
                        </c:forEach>
                    </c:when>

                    <c:otherwise>
                        <c:forEach var="i" begin="0" end="9">
                            <div class="slot-row">
                                <button type="button"
                                        class="slot-btn"
                                        id="slot-${i}"
                                        data-slot="${i}"
                                        disabled
                                        aria-disabled="true">
                                    <div class="slot-label">Слот ${i + 1} — пусто</div>
                                </button>

                                <form method="post" action="${loadsAction}" style="margin:0">
                                    <input type="hidden" name="${PARAM_OP}" value="${OP_DELETE}"/>
                                    <input type="hidden" name="${PARAM_SLOT}" value="${i}"/>
                                    <input type="hidden" name="${PARAM_NEXT}" value="${param[PARAM_NEXT]}"/>
                                    <input type="hidden" name="${PARAM_PURPOSE}" value="${param[PARAM_PURPOSE]}"/>
                                    <input type="hidden" name="${PARAM_NODE}" value="${param[PARAM_NODE]}"/>
                                    <button type="submit" class="btn btn-danger slot-delete">Удалить</button>
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