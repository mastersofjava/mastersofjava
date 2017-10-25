var filesArray = [];
var testsArray = [];


[# th:if="${running} and not ${finished}"]
[# th:each="file : ${files}"]

if ([# th:text="|${file.fileType.name()}|"/] == 'EDIT'  || [# th:text="|${file.fileType.name()}|"/] == 'READONLY') {

	var [# th:utext="${file.name}"/] = new CodeMirror(document.getElementById([# th:text="|${file.name}|"/]), {
		lineNumbers: true,
		matchBrackets: true,
		mode: "text/x-java",
		readOnly: [# th:text="|${file.readOnly}|"/]
	  });
	filesArray.push({ filename:[# th:text="|${file.filename}|"/]
					, cmEditor: [# th:utext="${file.name}"/]
					, readonly: [# th:text="|${file.readOnly}|"/]
					, fileType: [# th:text="|${file.fileType.name()}|"/]
					, name: [# th:text="${file.name}"/]
					});

		
	[# th:utext="${file.name}"/].setValue([# th:text="${file.content}"/]);
	[# th:utext="${file.name}"/].setSize('100%', 500);
	
} else {
	filesArray.push(null);
		
	if ([# th:text="|${file.fileType.name()}|"/] == 'TASK') {
		var x = document.getElementById([# th:text="|${file.name}|"/]);
		x.innerHTML = "<textarea rows='30' cols='100'>" + [# th:text="|${file.content}|"/] + "</textarea>";
	} else {
		testsArray.push([# th:text="|${file.name}|"/]);
		var x = document.getElementById([# th:text="|${file.name}|"/]);
		x.innerHTML = '<pre></pre>';
	}
}

[/]  
$('#tabs').bind('tabsactivate',function(e, ui) {
	if (filesArray[ui.newTab.index()] !=  null) {
		filesArray[ui.newTab.index()].cmEditor.refresh();
	}
});
	
[/]  
	
