var cmArray = [];


[# th:each="file : ${files}"]
var readOnly = true;
if ([# th:text="${file}"/] == "opgave") {
	readOnly = false;
}
var [# th:utext="${file}"/] = new CodeMirror(document.getElementById([# th:text="|${file}.java|"/]), {
	lineNumbers: true,
	matchBrackets: true,
	mode: "text/x-java",
	readOnly: readOnly
  });
cmArray.push([# th:utext="${file}"/]);

$.get( "opgave.txt", function( data ) {
	[# th:utext="${file}"/].setValue(data);
	});

//if ([# th:text="${file}"/] == "opgave") {
//	$([# th:utext="${file}"/].getWrapperElement()).show();
//} else {
//	$([# th:utext="${file}"/].getWrapperElement()).hide();
//}

//console.log($([# th:utext="${file}"/].getWrapperElement()));

[/]  

	$('#tabs').bind('tabsactivate',function(e, ui) {
//		$(ui.oldPanel).find(".cm-s-default").hide();
//		$(ui.newPanel).find(".cm-s-default").show();
		
		var curTab = $('.ui-state-active');
		console.log(curTab.index());
		cmArray[curTab.index()].refresh();
		
		$(ui.newPanel).find(".cm-s-default")
	});
	
	
//var mac = CodeMirror.keyMap.default == CodeMirror.keyMap.macDefault;
//CodeMirror.keyMap.default[(mac ? "Cmd" : "Ctrl") + "-Space"] = "autocomplete";

	
	
