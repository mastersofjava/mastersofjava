	var stompClient = null;

    function getContent() {
		var curTab = $('.ui-state-active');
		console.log(curTab.index());
		return cmArray[curTab.index()].getValue();
    }  
      
	function setConnected(connected) {
		document.getElementById('response').innerHTML = '';
	}

	function connect() {

		var socket = new SockJS('/submit');
		stompClient = Stomp.over(socket);

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
			'source' : getContent()
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
		var p = document.createElement('p');
		p.style.wordWrap = 'break-word';
		p.appendChild(document.createTextNode(messageOutput.team + ": "
				+ messageOutput.text + " (" + messageOutput.time + ")"));
		response.appendChild(p);
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
