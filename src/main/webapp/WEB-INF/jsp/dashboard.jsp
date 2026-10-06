<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="ダッシュボード" />
<%@ include file="common/header.jspf" %>
<h1>ダッシュボード <small>運用日 <c:out value="${operationDate}" /></small></h1>

<div class="tiles">
  <sec:authorize access="hasAnyRole('DISPATCHER','MAINTAINER','ADMIN')">
  <div class="tile <c:if test='${not empty unackedAlarms}'>alert</c:if>">未確認警報<span class="big" id="tile-alarms">${fn:length(unackedAlarms)}</span>うち重故障 ${majorAlarmCount}</div>
  <div class="tile">停電作業 承認待ち<span class="big" id="tile-outages">${pendingOutageCount}</span>&nbsp;</div>
  </sec:authorize>
  <div class="tile <c:if test='${not empty inspectionAttention}'>alert</c:if>">検査期限 注意・超過<span class="big" id="tile-inspections">${fn:length(inspectionAttention)}</span>編成</div>
  <div class="tile">未完了の故障<span class="big" id="tile-failures">${openFailureCount}</span>件</div>
  <div class="tile">メーカー修理中<span class="big" id="tile-repairs">${fn:length(openRepairs)}</span>件</div>
</div>

<div class="row">
<sec:authorize access="hasAnyRole('DISPATCHER','MAINTAINER','ADMIN')">
<div class="panel half">
  <h2>変電所 最新計測値</h2>
  <table class="grid">
    <thead><tr><th>変電所</th><th>計測時刻</th><th>電圧 [V]</th><th>電流 [A]</th></tr></thead>
    <tbody>
    <c:forEach var="s" items="${substations}">
      <c:if test="${s.telemetryEnabled}">
      <c:set var="m" value="${latest[s.code]}" />
      <tr>
        <td><a href="${ctx}/power/substations/${s.code}"><c:out value="${s.code}" /> <c:out value="${s.name}" /></a></td>
        <td><fmt:formatDate value="${m.measuredAt}" pattern="MM/dd HH:mm" /></td>
        <td class="num"><fmt:formatNumber value="${m.voltageV}" /></td>
        <td class="num"><fmt:formatNumber value="${m.currentA}" /></td>
      </tr>
      </c:if>
    </c:forEach>
    </tbody>
  </table>
  <h2 style="margin-top:12px">未確認警報</h2>
  <table class="grid">
    <thead><tr><th>発生</th><th>変電所</th><th>レベル</th><th>内容</th></tr></thead>
    <tbody>
    <c:forEach var="a" items="${unackedAlarms}">
      <tr><td><fmt:formatDate value="${a.occurredAt}" pattern="MM/dd HH:mm" /></td><td>${a.substationCode}</td>
          <td><span class="lv-${a.level}">${a.level.label}</span></td><td><c:out value="${a.message}" /></td></tr>
    </c:forEach>
    <c:if test="${empty unackedAlarms}"><tr><td colspan="4" class="empty">未確認の警報はありません</td></tr></c:if>
    </tbody>
  </table>
</div>
</sec:authorize>

<div class="panel half">
  <h2>検査期限 注意・超過</h2>
  <table class="grid">
    <thead><tr><th>編成</th><th>次回検査</th><th>期限</th><th>残日数</th><th>判定</th></tr></thead>
    <tbody>
    <c:forEach var="d" items="${inspectionAttention}">
      <tr><td><a href="${ctx}/rolling/formations/${d.formationNo}">${d.formationNo}</a></td><td>${d.nextKind.label}</td>
          <td>${d.nextDueText}</td><td class="num">${d.daysRemaining}</td>
          <td><span class="judge-${d.judge}">${d.judge.label}<c:if test="${d.kmExceeded}"> (走行km)</c:if></span></td></tr>
    </c:forEach>
    <c:if test="${empty inspectionAttention}"><tr><td colspan="5" class="empty">該当なし</td></tr></c:if>
    </tbody>
  </table>
  <h2 style="margin-top:12px">メーカー修理中</h2>
  <table class="grid">
    <thead><tr><th>依頼番号</th><th>故障</th><th>メーカー</th><th>進捗</th></tr></thead>
    <tbody>
    <c:forEach var="r" items="${openRepairs}">
      <tr><td>${r.orderNo}</td><td><a href="${ctx}/rolling/failures/${r.failure.failureNo}">${r.failure.failureNo}</a></td>
          <td><c:out value="${r.maker}" /></td><td class="st-${r.status}">${r.status.label}</td></tr>
    </c:forEach>
    <c:if test="${empty openRepairs}"><tr><td colspan="4" class="empty">該当なし</td></tr></c:if>
    </tbody>
  </table>
</div>
</div>
<%@ include file="common/footer.jspf" %>
