	var stompClient = null;

    function getContent() {
		var curTab = $('.ui-state-active');
		console.log(curTab.index());
		var editables = [];
		for(let i = 0; i < filesArray.length; i++){
			if (!filesArray[i].readonly) {
				var file = {filename: filesArray[i].filename, content: filesArray[i].cmEditor.getValue()}
				editables.push(file);				
			}
		}
		
		return editables;
    }  
      
	function setConnected(connected) {
		document.getElementById('response').innerHTML = '';
	}

	function connect() {

		var socket = new SockJS('/submit');
		stompClient = Stomp.over(socket);
		stompClient.debug = null;
		stompClient.connect({}, function(frame) {

			setConnected(true);
			console.log('Connected: ' + frame);
			stompClient.subscribe('/topic/messages', function(messageOutput) {
				console.log("topic messages")
				showMessageOutput(JSON.parse(messageOutput.body));
			});
			
			stompClient.subscribe('/user/queue/feedback', function(messageOutput) {
				console.log("user feedback")
				showMessageOutput(JSON.parse(messageOutput.body));
			});

		});
	}

	function disconnect() {

		if (stompClient != null) {
			stompClient.disconnect();
		}

		setConnected(false);
		console.log("Disconnected");
	}
	
	function compile() {
		
		stompClient.send("/app/submit/compile", {}, JSON.stringify({
			'team' : 'team1',
			'source' :  getContent()
		}));
	}

	function test() { 
		stompClient.send("/app/submit/test", {}, JSON.stringify({
			'team' : 'team1',
			'source' : getContent()
		}));
	}

	
	function showMessageOutput(messageOutput) {
		console.log("show");
		var response = document.getElementById('response');
		response.insertBefore(document.createElement('hr'), response.firstElementChild);
		var p = document.createElement('p');
		p.appendChild(document.createTextNode(messageOutput.time));
		response.insertBefore(p, response.firstElementChild);
		
		var pre = document.createElement('pre');
		pre.appendChild(document.createTextNode(messageOutput.text));
		response.insertBefore(pre, response.firstElementChild);
		
		p = document.createElement('p');
		p.appendChild(document.createTextNode(messageOutput.team + ": "));
		response.insertBefore(p, response.firstElementChild);
	}
	
	function showOutput(messageOutput) {
		var response = document.getElementById('response');
		var p = document.createElement('p');
		p.style.wordWrap = 'break-word';
		p.appendChild(document.createTextNode(messageOutput));
		response.appendChild(p);
	}
	
	function getAssignmentFiles(){
		$.ajax({url: "/files", success: function(result){
	        //$("#div1").html(result);
			showOutput(result);
	    }});
	}
