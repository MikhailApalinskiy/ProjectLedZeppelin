<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!doctype html>
<html lang="ru">
<head>
    <meta charset="UTF-8"/>
    <title>Паутина квеста — TextQuest</title>

    <c:url var="cssMain" value="/assets/css/main.css"/>
    <c:url var="cssGraph" value="/assets/css/graph.css"/>
    <link rel="stylesheet" href="${cssMain}"/>
    <link rel="stylesheet" href="${cssGraph}"/>

    <c:set var="PATH_HOME" value="/"/>
    <c:set var="PATH_EDITOR" value="/create_quest"/>
    <c:set var="PATH_GRAPH" value="/quest/graph_svg"/>
    <c:set var="PATH_PUBLISH" value="/quest/publish"/>
    <c:set var="P_ID" value="id"/>
    <c:set var="MARKER_ID" value="arrow"/>

    <c:url var="homeUrl" value="${PATH_HOME}"/>
    <c:url var="editorUrl" value="${PATH_EDITOR}"/>
    <c:url var="selfUrl" value="${PATH_GRAPH}"/>
    <c:url var="publishUrl" value="${PATH_PUBLISH}"/>
</head>
<body class="page-graph">

<header class="app-header">
    <div class="app-header__inner">
        <div class="brand">
            <span class="logo" aria-hidden="true"></span>
            <span>TextQuest — Паутина</span>
        </div>
        <div class="header-actions">
            <a class="btn btn-ghost" href="${homeUrl}">На главную</a>
            <a class="btn" href="${editorUrl}">Редактор</a>
            <a class="btn" href="${selfUrl}">Перерисовать</a>

            <c:set var="editingId" value="${sessionScope.editingQuestId}"/>
            <c:set var="editingName" value="${sessionScope.editingQuestName}"/>
            <c:choose>
                <c:when test="${not empty editingId}">
                    <a class="btn btn-primary" href="${publishUrl}">Сохранить изменения</a>
                </c:when>
                <c:otherwise>
                    <a class="btn btn-primary" href="${publishUrl}">Опубликовать</a>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</header>

<main class="container fade-in" role="main">
    <article class="card">
        <header class="card-header">
            <h1>Паутина квеста</h1>
            <c:if test="${not empty editingId}">
                <span class="badge">Редактируется:
                    <c:out value="${empty editingName ? 'Без названия' : editingName}"/>
                    (id: <c:out value="${editingId}"/>)</span>
            </c:if>
        </header>

        <section class="card-body">
            <%@ include file="/WEB-INF/jsp/fragments/alerts.jspf" %>

            <c:choose>
                <c:when test="${isEmpty}">
                    <p class="notice">Пока нет данных для отображения. Создайте узлы в редакторе.</p>
                </c:when>
                <c:otherwise>
                    <svg viewBox="0 0 ${width} ${height}" role="img" aria-label="Граф квеста"
                         xmlns="http://www.w3.org/2000/svg">
                        <defs>
                            <marker id="${MARKER_ID}" viewBox="0 0 10 10" refX="10" refY="5"
                                    markerWidth="8" markerHeight="8" orient="auto-start-reverse">
                                <path d="M 0 0 L 10 5 L 0 10 z"/>
                            </marker>
                        </defs>

                        <c:forEach var="e" items="${edges}">
                            <line class="edge" x1="${e.sx}" y1="${e.sy}" x2="${e.tx}" y2="${e.ty}"
                                  marker-end="url(#${MARKER_ID})"/>
                            <text class="edge-label"
                                  x="${(e.sx + e.tx) / 2}" y="${(e.sy + e.ty) / 2 - 6}"
                                  text-anchor="middle">
                                <c:out value="${e.label}"/>
                            </text>
                        </c:forEach>

                        <c:forEach var="n" items="${positions}">
                            <c:set var="CLS_NODE" value="node"/>
                            <c:set var="CLS_FIN" value="fin"/>
                            <c:set var="CLS_START" value="start"/>

                            <c:set var="cls" value="${CLS_NODE}"/>
                            <c:if test="${n.fin}"><c:set var="cls" value="${cls} ${CLS_FIN}"/></c:if>
                            <c:if test="${n.start}"><c:set var="cls" value="${cls} ${CLS_START}"/></c:if>

                            <c:url var="editLink" value="${PATH_EDITOR}">
                                <c:param name="${P_ID}" value="${n.id}"/>
                            </c:url>

                            <a href="${editLink}">
                                <g>
                                    <rect class="${cls}" x="${n.x}" y="${n.y}" rx="10" ry="10" width="${nodeW}"
                                          height="${nodeH}"/>

                                    <c:if test="${not empty n.image}">
                                        <c:set var="thumbX" value="${n.x + nodeW - 38}"/>
                                        <c:set var="thumbY" value="${n.y + 6}"/>
                                        <c:set var="clipId" value="thumbClip_${n.id}"/>
                                        <defs>
                                            <clipPath id="${clipId}">
                                                <rect x="${thumbX}" y="${thumbY}" width="32" height="32" rx="6" ry="6"/>
                                            </clipPath>
                                        </defs>
                                        <image
                                                href="${pageContext.request.contextPath}${n.image}"
                                                x="${thumbX}" y="${thumbY}"
                                                width="32" height="32"
                                                preserveAspectRatio="xMidYMid slice"
                                                clip-path="url(#${clipId})"/>
                                        <rect
                                                x="${thumbX}" y="${thumbY}"
                                                width="32" height="32"
                                                rx="6" ry="6"
                                                class="node-thumb-border"/>
                                    </c:if>

                                    <text class="label id-label" x="${n.x + nodeW/2}" y="${n.y + 18}"
                                          text-anchor="middle">
                                        #<c:out value="${n.id}"/><c:if test="${n.fin}"> ⓕ</c:if>
                                    </text>

                                    <c:forEach var="line" items="${n.labelLines}" varStatus="st">
                                        <text class="snippet"
                                              x="${n.x + nodeW/2}"
                                              y="${n.y + 18 + (st.index + 1) * 14}"
                                              text-anchor="middle">
                                            <c:out value="${line}"/>
                                        </text>
                                    </c:forEach>
                                </g>
                            </a>
                        </c:forEach>
                    </svg>

                    <p class="notice" style="margin-top:10px">
                        Стартовый узел — зелёная рамка. Финальные — фиолетовые. Клик по узлу — открыть его в редакторе.
                    </p>
                </c:otherwise>
            </c:choose>
        </section>
    </article>
</main>
</body>
</html>