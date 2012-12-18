select substr(accessurl, 8, LOCATE('/',substr(accessurl, 8))-1) as host, archiveresult, count(1) c
from archivedlink
where archiveresult!='success' and archiveresult!='Invalid snapshot ID' and LOCATE('/',substr(accessurl, 8)) > 0
group by substr(accessurl, 8, LOCATE('/',substr(accessurl, 8))-1), archiveresult
order by c desc;

delete from archivedlink
where LOCATE('/',substr(accessurl, 8)) > 0 and substr(accessurl, 8, LOCATE('/',substr(accessurl, 8))-1)='walkspb.ru' and archiveresult='failure_404';