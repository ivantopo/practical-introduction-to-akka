$(document).ready(function() {

  var game = new Phaser.Game(800, 600, Phaser.AUTO, '', { preload: preload, create: create, update: update });
  var localPlayerID = _.random(1, 10000);
  var platforms;
  var player;
  var players = {};
  var cursors;


  var sendState = _.throttle(function(x, y) {
    $.ajax({
      url: '/player/' + localPlayerID + '/state',
      type:"POST",
      data: JSON.stringify({ xPosition: x, yPosition: y }),
      contentType:"application/json; charset=utf-8",
      dataType:"json",
      success: function() { }
    });
  }, 30);


  function preload() {
    game.load.image('sky', '/assets/sky.png');
    game.load.image('ground', '/assets/platform.png');
    game.load.image('star', '/assets/star.png');
    game.load.spritesheet('dude', '/assets/dude.png', 32, 48);

  }

  function create() {
    game.physics.startSystem(Phaser.Physics.ARCADE);
    game.add.sprite(0, 0, 'sky');
    platforms = game.add.group();
    platforms.enableBody = true;

    var ground = platforms.create(0, game.world.height - 64, 'ground');
    ground.scale.setTo(2, 2);
    ground.body.immovable = true;

    var ledge = platforms.create(400, 400, 'ground');
    ledge.body.immovable = true;
    ledge = platforms.create(-150, 250, 'ground');
    ledge.body.immovable = true;


    player = buildPlayer(localPlayerID, _.random(0, 750), 0)
    cursors = game.input.keyboard.createCursorKeys();


    var eventSource = new EventSource('/join?player_id=' + localPlayerID + '&initial_x=' + player.x + '&initial_y=' + player.y);
    eventSource.onmessage = function(event) {
      var eventData = JSON.parse(event.data);
      console.log('arrived: ');
      console.log(eventData);
      serverUpdate(eventData)
    };
  }


  function buildPlayer(playerID, x, y) {
    var newPlayer = game.add.sprite(x, y, 'dude');
    game.physics.arcade.enable(newPlayer);

    newPlayer.body.bounce.y = 0.2;
    newPlayer.body.gravity.y = 600;
    newPlayer.body.collideWorldBounds = true;
    newPlayer.animations.add('left', [0, 1, 2, 3], 10, true);
    newPlayer.animations.add('right', [5, 6, 7, 8], 10, true);

    players[playerID] = newPlayer;
    return newPlayer;
  }

  function serverUpdate(event) {
    _.keys(event.changes).map(function(playerID) {
      if(!_.isUndefined(players[playerID])) {
        players[playerID].x = event.changes[playerID].xPosition;
        players[playerID].y = event.changes[playerID].yPosition;
      } else {
        buildPlayer(playerID, event.changes[playerID].xPosition, event.changes[playerID].yPosition + 1)
      }
    })

    // TODO: Send the acks
  }

  var lastState = {
    x: 0,
    y: 0
  }

  function update() {
    _(players).values().map(function (p) {
      game.physics.arcade.collide(p, platforms);
    });


    player.body.velocity.x = 0;

    if (cursors.left.isDown)
    {
      //  Move to the left
      player.body.velocity.x = -150;

      player.animations.play('left');
    }
    else if (cursors.right.isDown)
    {
      //  Move to the right
      player.body.velocity.x = 150;

      player.animations.play('right');
    }
    else
    {
      //  Stand still
      player.animations.stop();

      player.frame = 4;
    }

    //  Allow the player to jump if they are touching the ground.
    if (cursors.up.isDown)// && player.body.touching.down)
    {
      player.body.velocity.y = -350;
    }

    if(player.x != lastState.x || player.y != lastState.y) {
      sendState(player.x, player.y);
      lastState = {
        x: player.x,
        y: player.y
      }
    }

  }
});