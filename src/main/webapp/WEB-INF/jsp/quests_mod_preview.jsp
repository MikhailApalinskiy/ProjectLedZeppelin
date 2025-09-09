<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!doctype html>
<html lang="ru">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title>Предпросмотр паутины — TextQuest</title>

    <c:url var="cssMain" value="/assets/css/main.css"/>
    <c:url var="cssGraph" value="/assets/css/mod-graph.css"/>
    <link rel="stylesheet" href="${cssMain}"/>
    <link rel="stylesheet" href="${cssGraph}"/>
</head>
<body class="page-graph">
<main class="container">

    <c:set var="TITLE" value="${empty previewTitle ? 'Предпросмотр квеста' : previewTitle}"/>
    <c:set var="BACK_URL" value="${empty backUrl ? pageContext.request.contextPath : backUrl}"/>

    <c:set var="SVG_W" value="${empty width ? 960 : width}"/>
    <c:set var="SVG_H" value="${empty height ? 680 : height}"/>
    <c:set var="NODE_W" value="${empty nodeW ? 260 : nodeW}"/>
    <c:set var="NODE_H" value="${empty nodeH ? 90 : nodeH}"/>

    <c:set var="RADIUS" value="10"/>
    <c:set var="ARROW" value="6"/>
    <c:set var="THUMB" value="44"/>
    <c:set var="THUMB_PAD" value="8"/>
    <c:set var="THUMB_BOX" value="54"/>
    <c:set var="TEXT_LPAD" value="10"/>
    <c:set var="LINE_Y0" value="34"/>
    <c:set var="LINE_H" value="14"/>

    <article class="card" aria-labelledby="title">
        <header class="card-header">
            <div>
                <h1 id="title" class="title mb-1"><c:out value="${TITLE}"/></h1>
                <p class="muted m-0">Только просмотр. Переходы и редактирование недоступны.</p>
            </div>
            <div class="actions">
                <a class="btn btn-ghost" href="${BACK_URL}">Назад к модерации</a>
            </div>
        </header>

        <section class="card-body">
            <c:choose>
                <c:when test="${isEmpty}">
                    <p class="muted">Черновик пуст — узлы не найдены.</p>
                </c:when>
                <c:otherwise>
                    <div class="svg-wrap">
                        <svg xmlns="http://www.w3.org/2000/svg"
                             width="${SVG_W}" height="${SVG_H}"
                             viewBox="0 0 ${SVG_W} ${SVG_H}"
                             role="img" aria-label="Граф квеста">

                            <defs>
                                <marker id="arrow" viewBox="0 0 ${ARROW} ${ARROW}"
                                        refX="${ARROW}" refY="${ARROW/2}"
                                        markerWidth="${ARROW}" markerHeight="${ARROW}"
                                        orient="auto-start-reverse">
                                    <path d="M0,0 L${ARROW},${ARROW/2} L0,${ARROW} z"/>
                                </marker>

                                <clipPath id="thumbClip">
                                    <rect x="0" y="0" width="${THUMB}" height="${THUMB}" rx="8" ry="8"/>
                                </clipPath>
                            </defs>

                            <c:forEach var="e" items="${edges}">
                                <line class="edge"
                                      x1="${e.sx}" y1="${e.sy}"
                                      x2="${e.tx}" y2="${e.ty}"
                                      marker-end="url(#arrow)"/>
                                <text class="edge-label"
                                      x="${(e.sx + e.tx) / 2}"
                                      y="${(e.sy + e.ty) / 2 - 4}">
                                    <c:out value="${e.label}"/>
                                </text>
                            </c:forEach>

                            <c:forEach var="p" items="${positions}">
                                <rect class="node ${p.fin ? 'fin' : ''} ${p.start ? 'start' : ''}"
                                      x="${p.x}" y="${p.y}"
                                      width="${NODE_W}" height="${NODE_H}"
                                      rx="${RADIUS}" ry="${RADIUS}"/>

                                <text class="node-id"
                                      x="${p.x + 10}" y="${p.y + 16}">
                                    #<c:out value="${p.id}"/>
                                </text>

                                <c:if test="${not empty p.image}">
                                    <c:set var="imgHref" value="${p.image}"/>
                                    <c:if test="${fn:startsWith(imgHref, '/')}">
                                        <c:set var="imgHref" value="${pageContext.request.contextPath}${p.image}"/>
                                    </c:if>

                                    <g transform="translate(${p.x + NODE_W - THUMB_BOX}, ${p.y + THUMB_PAD})">
                                        <rect class="node-thumb-border"
                                              x="0" y="0" width="${THUMB}" height="${THUMB}" rx="8" ry="8"/>
                                        <image href="${imgHref}" x="0" y="0"
                                               width="${THUMB}" height="${THUMB}"
                                               preserveAspectRatio="xMidYMid slice"
                                               clip-path="url(#thumbClip)"/>
                                    </g>
                                </c:if>

                                <c:set var="lineY" value="${p.y + LINE_Y0}"/>
                                <c:forEach var="ln" items="${p.labelLines}" varStatus="st">
                                    <text class="node-text"
                                          x="${p.x + TEXT_LPAD}"
                                          y="${lineY + st.index * LINE_H}">
                                        <c:out value="${ln}"/>
                                    </text>
                                </c:forEach>
                            </c:forEach>
                        </svg>
                    </div>
                </c:otherwise>
            </c:choose>
        </section>
    </article>
</main>
</body>
</html>
