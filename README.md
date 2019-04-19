# API

## Create a new game
`POST /minesweepers/`
### Successful response
Status: 200
Body: `minesweeper-structure`

Where `minesweeper-structure` is:
```
{
	"id": id,
	"creation_date_time": "2019-04-18T22:15:53.006Z",
	"updated_date_time": "2019-04-18T22:15:53.006Z",
	"status": status,
	"field": [[cell-value*]*],
	"pauses": []
}
```
and `id` is a random alphanumeric string identifying the game created,
and `status` is a string corresponding to the current game status whose possible values are:
* new
* playing
* paused
* killed
* won

and field `field` is a bi-dimensional array of `cell-value`s,
and `cell-value` is a json string with one of the following values:
* unknown (cell not shovelled nor marked)
* bomb mark
* question mark
* bomb (user shovelled on bomb spot, losing the game)
* 0-8 (shovelled spot with show number of surrounding bombs)

When a new game is created, every `cell-value` is `unknown`. This means that no cell has been shovelled nor marked yet.

#### Example:
```
{
	"id": "YLsXq7HYms",
	"creation_date_time": "2019-04-18T22:15:53.006Z",
	"updated_date_time": "2019-04-18T22:15:53.006Z",
	"status": "new",
	"field": [
		["unknown", "unknown", "unknown", "unknown", "unknown"],
		["unknown", "unknown", "unknown", "unknown", "unknown"],
		["unknown", "unknown", "unknown", "unknown", "unknown"],
		["unknown", "unknown", "unknown", "unknown", "unknown"],
		["unknown", "unknown", "unknown", "unknown", "unknown"]
	],
	"pauses": []
}
```

## Get current game state
`GET /minesweepers/:id`
### Successful response
Status: 200
Body: `minesweeper-structure`
(The body of this response is equal to the body of a 200 response of creating a new game.)
### Failure response: no game found for given id
Status: 404

## Put a bomb mark or a question mark
`POST /minesweepers/:id/minefield/x/y/mark` (bomb mark)
`POST /minesweepers/:id/minefield/x/y/mark?question` (question mark)
### Successful response
Status: 200
Body: `move-structure`

Were `move-structure` is:
```
{
	"minesweeper": miesweeper-structure,
	"cell_changes": [cell_change],
}
```
and `cell_change` is 
```
{
    coordinates: coordinates-structure,
    cell: "bomb mark" | "question mark"
}
```
and `coordinates-structure` is:
```
{
    x: number from 0 to width-1,
    y: number from 0 to length-1
}
```
#### Example:
POST /minesweepers/YLsXq7HYms/minefield/0/4/mark
Response status: 200
Response body:
```
{
	"minesweeper": {
		"id": "YLsXq7HYms",
		"creation_date_time": "2019-04-18T22:15:53.006Z",
		"updated_date_time": "2019-04-18T22:15:53.006Z",
		"status": "playing",
		"field": [
			["unknown", "unknown", "unknown", "unknown", "bomb mark"],
			["unknown", "unknown", "unknown", "unknown", "unknown"],
			["unknown", "unknown", "unknown", "unknown", "unknown"],
			["unknown", "unknown", "unknown", "unknown", "unknown"],
			["unknown", "unknown", "unknown", "unknown", "unknown"]
		],
		"pauses": []
	},
	"cell_changes": [{
		"coordinates": {
			"x": 0,
			"y": 4
		},
		"cell": "bomb mark"
	}]
}
```
### Failure response: trying to mark a spot that has already been shovelled
Status: 400
Body: spot-already-shovelled-structure

Where `spot-already-shovelled-structure` is:
```
{
    "failure": "spot already shovelled",
    "minesweeper": miesweeper-structure
}
```
### Failure response: game already ended
Status: 409
Body: game-over-structure

Where `game-over-structure` is:
```
{
    "reason": "game over"
    "minesweeper": ...
}

```
### Failure response: game paused
Status: 409
Body: game-paused-structure

Where `game-paused-structure` is:
```
{
    "reason": "game paused",
    "minesweeper": ...
}

```

## Remove bomb/question mark
`DELETE /minesweepers/:id/minefield/x/y/mark`
### Successful response
Status: 200
Body: `move-structure`
#### Example:
DELETE /minesweepers/YLsXq7HYms/minefield/0/4/mark
Response status: 200
Response body:
```
{
	"minesweeper": {
		"id": "YLsXq7HYms",
		"creation_date_time": "2019-04-18T22:15:53.006Z",
		"updated_date_time": "2019-04-18T22:15:53.006Z",
		"status": "playing",
		"field": [
			["unknown", "unknown", "unknown", "unknown", "unknown"],
			["unknown", "unknown", "unknown", "unknown", "unknown"],
			["unknown", "unknown", "unknown", "unknown", "unknown"],
			["unknown", "unknown", "unknown", "unknown", "unknown"],
			["unknown", "unknown", "unknown", "unknown", "unknown"]
		],
		"pauses": []
	},
	"cell_changes": [{
		"coordinates": {
			"x": 0,
			"y": 4
		},
		"cell": "unknown"
	}]
}
```
### Failure response: trying to un-mark a spot that has already been shovelled
Status: 400
Body: spot-already-shovelled-structure
### Failure response: game already ended
Status: 409
Body: game-over-structure
### Failure response: game paused
Status: 409
Body: game-paused-structure

## Shovel spot (hoping no underlying bomb explodes)
`POST /minesweepers/:id/minefield/x/y/shovel`
### Successful response
Status: 200
Body: `move-structure`
Additional info: when shovelling a cell with no bombs surrounding it, neighbour cells get recursively shovelled. 
#### Example:
POST /minesweepers/YLsXq7HYms/minefield/0/4/shovel
Response status: 200
Response body:
```
{
	"minesweeper": {
		"id": "YLsXq7HYms",
		"creation_date_time": "2019-04-18T22:15:53.006Z",
		"updated_date_time": "2019-04-18T22:15:53.006Z",
		"status": "playing",
		"field": [
			["unknown", "unknown", "unknown", "1"      , "0"],
			["unknown", "unknown", "unknown", "1"      , "0"],
			["unknown", "unknown", "unknown", "2"      , "1"],
			["unknown", "unknown", "unknown", "unknown", "unknown"],
			["unknown", "unknown", "unknown", "unknown", "unknown"]
		],
		"pauses": []
	},
	"cell_changes": [
        {
            "coordinates": { "x": 0, "y": 4 },
            "cell": "0"
        },
        {
            "coordinates": { "x": 0, "y": 3 },
            "cell": "1"
        },
        {
            "coordinates": { "x": 1, "y": 3 },
            "cell": "1"
        },
        {
            "coordinates": { "x": 1, "y": 4 },
            "cell": "0"
        },
        {
            "coordinates": { "x": 2, "y": 3 },
            "cell": "2"
        },
        {
            "coordinates": { "x": 2, "y": 4 },
            "cell": "1"
        }
    ]
}
```
### Failure response: trying to shovel a spot that has already been shovelled
Status: 400
Body: spot-already-shovelled-structure
### Failure response: trying to shovel a spot that has been marked
Status: 400
Body: spot-marked-structure

Where `spot-marked-structure` is:
```
{
    "failure": "can't shovel a marked spot",
    "minesweeper": miesweeper-structure
}

```
### Failure response: game already ended
Status: 409
Body: game-over-structure
### Failure response: game paused
Status: 409
Body: game-paused-structure

## Pause
`POST /minesweepers/:id/pause`
### Successful response
Status: 200
### Failure response: game already ended
Status: 409
Body: game-over-structure
### Failure response: game already paused
Status: 409
Body: game-paused-structure

## Resume
`POST /minesweepers/:id/resume`
### Successful response
Status: 200
### Failure response: game already ended
Status: 409
Body: game-over-structure
### Failure response: game not paused
Status: 409
Body: game-not-paused-structure

Where `game-not-paused-structure` is:
```
{
    "reason": "game not paused",
    "minesweeper": ...
}

```