<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="故障・修理" />
<%@ include file="../common/header.jspf" %>
<h1>故障記録・メーカー修理</h1>
<div class="panel">
  <form method="get" class="inline-form search">
    <label>編成 <select name="formationNo"><option value="">(すべて)</option>
      <c:forEach var="f" items="${formations}"><option value="${f.formationNo}" ${param.formationNo == f.formationNo ? 'selected' : ''}>${f.formationNo}</option></c:forEach></select></label>
    <label>処置状況 <select name="status"><option value="">(すべて)</option>
      <c:forEach var="s" items="${statuses}"><option value="${s}" ${param.status == s ? 'selected' : ''}>${s.label}</option></c:forEach></select></label>
    <label>重要度 <select name="severity"><option value="">(すべて)</option>
      <c:forEach var="s" items="${severities}"><option value="${s}" ${param.severity == s ? 'selected' : ''}>${s.label}</option></c:forEach></select></label>
    <button type="submit">検索</button>
    <sec:authorize access="hasAnyRole('MAINTAINER','ADMIN')"><a class="button" href="${ctx}/rolling/failures/new">故障登録</a></sec:authorize>
  </form>
  <table class="grid sortable" id="failures">
    <thead><tr><th>故障番号</th><th>発生日時</th><th>編成</th><th>製造番号</th><th>機器</th><th>内容</th><th>コード</th><th>重要度</th><th>処置状況</th></tr></thead>
    <tbody>
    <c:forEach var="f" items="${failures}">
      <tr>
        <td><a href="${ctx}/rolling/failures/${f.failureNo}">${f.failureNo}</a></td>
        <td><fmt:formatDate value="${f.occurredAt}" pattern="yyyy/MM/dd HH:mm" /></td>
        <td><a href="${ctx}/rolling/formations/${f.formationNo}">${f.formationNo}</a></td>
        <td><a href="${ctx}/rolling/equipment/${f.equipment.serialNo}">${f.equipment.serialNo}</a></td>
        <td>${f.equipment.equipmentType.label}</td>
        <td><c:out value="${f.symptom}" /></td>
        <td><c:out value="${f.failureCode}" /></td>
        <td>${f.severity.label}</td>
        <td class="st-${f.status}">${f.status.label}</td>
      </tr>
    </c:forEach>
    <c:if test="${empty failures}"><tr><td colspan="9" class="empty">該当なし</td></tr></c:if>
    </tbody>
  </table>
</div>
<div class="panel">
  <h2>メーカー修理 進行中</h2>
  <table class="grid">
    <thead><tr><th>依頼番号</th><th>故障</th><th>メーカー</th><th>依頼日時</th><th>進捗</th><th>メモ</th></tr></thead>
    <tbody>
    <c:forEach var="r" items="${openRepairs}">
      <tr><td>${r.orderNo}</td><td><a href="${ctx}/rolling/failures/${r.failure.failureNo}">${r.failure.failureNo}</a></td><td><c:out value="${r.maker}" /></td>
          <td><fmt:formatDate value="${r.requestedAt}" pattern="yyyy/MM/dd HH:mm" /></td><td class="st-${r.status}">${r.status.label}</td><td><c:out value="${r.progressNote}" /></td></tr>
    </c:forEach>
    <c:if test="${empty openRepairs}"><tr><td colspan="6" class="empty">該当なし</td></tr></c:if>
    </tbody>
  </table>
</div>
<%@ include file="../common/footer.jspf" %>
