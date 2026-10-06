<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="${failure.failureNo}" />
<%@ include file="../common/header.jspf" %>
<h1>故障 ${failure.failureNo} <small class="st-${failure.status}">${failure.status.label}</small></h1>
<div class="row">
<div class="panel half">
  <table class="kv" id="failure">
    <tr><th>故障番号</th><td>${failure.failureNo}</td></tr>
    <tr><th>発生日時</th><td><fmt:formatDate value="${failure.occurredAt}" pattern="yyyy/MM/dd HH:mm" /></td></tr>
    <tr><th>編成</th><td><a href="${ctx}/rolling/formations/${failure.formationNo}">${failure.formationNo}</a></td></tr>
    <tr><th>機器</th><td><a href="${ctx}/rolling/equipment/${failure.equipment.serialNo}">${failure.equipment.serialNo}</a>
      ${failure.equipment.equipmentType.label} ${failure.equipment.model}</td></tr>
    <tr><th>メーカー</th><td><c:out value="${failure.equipment.maker}" /></td></tr>
    <tr><th>故障内容</th><td><c:out value="${failure.symptom}" /></td></tr>
    <tr><th>故障コード</th><td><c:out value="${failure.failureCode}" /></td></tr>
    <tr><th>重要度</th><td>${failure.severity.label}</td></tr>
    <tr><th>処置状況</th><td class="st-${failure.status}" id="failure-status">${failure.status.label}</td></tr>
    <tr><th>登録者</th><td><c:out value="${failure.reportedBy}" /></td></tr>
  </table>
  <sec:authorize access="hasAnyRole('MAINTAINER','ADMIN')">
  <p>
    <c:if test="${failure.status == 'OPEN'}">
      <form action="${ctx}/rolling/failures/${failure.failureNo}/investigate" method="post" class="inline">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" /><button type="submit">調査開始</button></form>
    </c:if>
    <c:if test="${failure.status == 'OPEN' || failure.status == 'INVESTIGATING'}">
      <form action="${ctx}/rolling/failures/${failure.failureNo}/repair" method="post" class="inline js-confirm" data-confirm="<c:out value='${failure.equipment.maker}' /> へ修理依頼しますか？">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" /><button type="submit" class="primary">メーカー修理依頼</button></form>
    </c:if>
    <c:if test="${failure.status != 'REPAIR_REQUESTED' && failure.status != 'CLOSED'}">
      <form action="${ctx}/rolling/failures/${failure.failureNo}/close" method="post" class="inline js-confirm" data-confirm="完了にしますか？">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" /><button type="submit">完了</button></form>
    </c:if>
  </p>
  </sec:authorize>
</div>
<div class="panel half">
  <h2>メーカー修理依頼</h2>
  <table class="grid" id="repairs">
    <thead><tr><th>依頼番号</th><th>メーカー</th><th>依頼</th><th>進捗</th><th>メモ</th><th>更新</th></tr></thead>
    <tbody>
    <c:forEach var="r" items="${repairs}">
      <tr><td>${r.orderNo}</td><td><c:out value="${r.maker}" /></td><td><fmt:formatDate value="${r.requestedAt}" pattern="MM/dd HH:mm" /> <c:out value="${r.requestedBy}" /></td>
          <td class="st-${r.status}">${r.status.label}</td><td><c:out value="${r.progressNote}" /></td>
          <td><c:out value="${r.updatedBy}" /> <fmt:formatDate value="${r.updatedAt}" pattern="MM/dd HH:mm" /></td></tr>
    </c:forEach>
    <c:if test="${empty repairs}"><tr><td colspan="6" class="empty">修理依頼なし</td></tr></c:if>
    </tbody>
  </table>
  <sec:authorize access="hasAnyRole('MAKER','ADMIN')">
  <c:forEach var="r" items="${repairs}">
    <c:if test="${r.status != 'RETURNED'}">
    <h2 style="margin-top:12px">進捗更新 (${r.orderNo})</h2>
    <form action="${ctx}/rolling/repairs/${r.orderNo}/progress" method="post" class="repair">
      <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
      <select name="status"><c:forEach var="s" items="${repairStatuses}"><option value="${s}" ${s == r.status ? 'selected' : ''}>${s.label}</option></c:forEach></select>
      <input type="text" name="note" size="40" maxlength="400" value="<c:out value='${r.progressNote}' />">
      <button type="submit" class="primary">更新</button>
    </form>
    </c:if>
  </c:forEach>
  </sec:authorize>
</div>
</div>
<%@ include file="../common/footer.jspf" %>
