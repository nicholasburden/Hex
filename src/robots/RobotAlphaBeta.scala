import moveordering.MoveOrdering
import hexagony._
import heuristic._
import hsearch._
import pierule._



//PROBLEM: WHEN LADDERING FROM RED ON 0 SIDE, IT DOES NOT GET STRONG CONNECTION RIGHT AFTER MAKE MOVE INTO STRONG CARRIER


class RobotAlphaBeta(model: Model, timelimit: Long, pierule: Boolean, colour: Colour)
  extends Robot(model: Model, timelimit: Long, pierule: Boolean, colour: Colour) {
  val DEPTH = 2
  val pieRule = new PieRule(model.N)
  val pieRuleTable = pieRule.getTable
  private def myMove(): Cell = {
    try {
      //playing middle is strong on first go
      if(model.count == 0 && !pierule) return model.board(model.N/2)(model.N/2 )
      val moveOrdering = new MoveOrdering
      val mod = model.copy()
      moveOrdering.initial(mod)
      val open = moveOrdering.getOrdering(mod)

      if(model.pie && !HSearch.p) HSearch.pie
      val alpha = Float.NegativeInfinity
      val beta = Float.PositiveInfinity
      var topScore = Float.NegativeInfinity


      val hme = new HSearch(mod, colour)
      val hthem = new HSearch(mod, othercolour)
      //if (model.pie) {hme.pie; hthem.pie}

      hme.initial
      hthem.initial

      hme.search
      hthem.search
      //if(colour.equals(R))
      for (cell1 <- open) {
        val cell = mod.board(cell1.i)(cell1.j)
        val mod2 = result(mod, cell, colour)
        val hme2 = hme.makeMove(cell.i, cell.j, colour)
        val hthem2 = hthem.makeMove(cell.i, cell.j, colour)
        if (!stop) {
          var score = 0.0f
          try {
            val mo = moveOrdering.addMovesFor(cell, mod)
            score = min(mod2, DEPTH - 1, alpha, beta, hme2, hthem2, mo)

            //check for case where opponent uses pie rule
            if(othercolour.equals(B) && mod2.count == 1 && pierule){
              //search for cell
              println("Y")
              //play pie

              val modPie = result(mod, cell, B)
              HSearch.pie
              modPie.pie = true
              hme.model.pie = true
              hthem.model.pie = true
              hthem.colour = R
              hme.colour = B

              val value = max(modPie, DEPTH-1, alpha, beta, hthem.makeMove(cell.i, cell.j, B), hme.makeMove(cell.i,cell.j, B), mo)
              //undo pie rule
              hthem.colour = B
              hme.colour = R
              hme.model.pie = false
              hthem.model.pie = false
              modPie.pie = false
              HSearch.pie
              score = Math.min(score, value)

            }


          } catch {
            case e: Exception => e.printStackTrace()
          }
          println(cell + " score = " + score)
          if ((score > topScore)) { // cell is a winning move
            move = cell;
            topScore = score
          }
        }
      }


      return move
    }catch{
      case e : Exception => e.printStackTrace()
    }
    return null
  }
  def min(model : Model, depth : Int, _alpha : Float, _beta : Float, hme : HSearch, hthem : HSearch, mo : MoveOrdering) : Float = {

    val alpha = _alpha
    var beta = _beta
    // println("Start next" + depth)
    if(model.solution(colour)){
      return Float.PositiveInfinity
    }
    else if(model.solution(othercolour)){
      return Float.MinValue
    }

    else if(depth == 0){
      //println("Heuristic")
      val heuristic = new ResistanceHeuristic
      hme.makeConnectionsConsistent()
      hthem.makeConnectionsConsistent()
      return heuristic.evaluate(model, colour, hme, hthem)

    }
    else{
      //println("Finished checking if leaf")
      var bestVal = Float.PositiveInfinity


      for (cell1 <- mo.getOrdering(model)){

        val cell = model.board(cell1.i)(cell1.j)
        val value = max(result(model, cell, othercolour), depth - 1, alpha, beta, hme.makeMove(cell.i, cell.j, othercolour), hthem.makeMove(cell.i, cell.j, othercolour), mo.addMovesFor(cell, model))

        bestVal = Math.min(bestVal, value)
        beta = Math.min(beta, bestVal)
        if (beta <= alpha){
          return bestVal
        }

      }


      return bestVal
    }
  }

  def max(model : Model, depth : Int, _alpha : Float, _beta : Float, hme : HSearch, hthem : HSearch, mo : MoveOrdering) : Float = {

    var alpha = _alpha
    val beta = _beta
    // println("Start next" + depth)
    if(model.solution(colour)){
      return Float.PositiveInfinity
    }
    else if(model.solution(othercolour)){
      return Float.MinValue
    }
    else if(depth == 0){
      val heuristic = new ResistanceHeuristic
      hme.makeConnectionsConsistent()
      hthem.makeConnectionsConsistent()
      return heuristic.evaluate(model, colour, hme, hthem)

    }
    else{
      // println("Finished checking if leaf")
      var bestVal = Float.NegativeInfinity

      for (cell1 <- mo.getOrdering(model)){
      //for(cell1 <- model.myCells(O)){
        val cell = model.board(cell1.i)(cell1.j)
        val value = min(result(model, cell, colour), depth - 1, alpha, beta, hme.makeMove(cell.i, cell.j, colour), hthem.makeMove(cell.i, cell.j, colour), mo.addMovesFor(cell, model))
        bestVal = Math.max(bestVal, value)
        alpha = Math.max(alpha, bestVal)
        if (beta <= alpha){
          return bestVal
        }

      }
      return bestVal
    }
  }

  // Your method for deciding whether to play the pie rule
  private def myPie(firstmove: Cell): Boolean = model.N <= 5 && pieRuleTable(firstmove.i)(firstmove.j)



  private def result(mod: Model, cell: Cell, col: Colour): Model = {
    val mod2 = mod.copy()
    mod2.playMove(cell, col)
    return mod2
  }



  // ------------------------------------------------------------------------------------------------
  /* The parameters passed to the robot are:
   * model - a blank copy of the game board model, complete with useful methods, updated by the controller
   * timelimit - the time (in milliseconds) that your robot has to complete their move
   * pierule - whether or not the pie rule is available to use after the first move
   * colour - the colour of your robot (R or B)
   *
   * Other values that are accessible to the robot include:
   * board - the game board in its current state
   * N - the size of the board
   * count - the number of tokens on the board
   * piePlayed - whether or not the pie rule has been played in this game
   * lastCell - the Cell on which the last move was played
   * othercolour - the colour of the other player
   *
   * You should reference the cell at coordinate (i, j) by the expression board(i)(j).
   * This value represents the cell at the given location on the board.
   * Coordinates are zero-indexed, meaning that they range from 0 to N - 1 in each direction.
   * board(i)(j) has attribute colour; model.colour(cell) also returns the colour of cell.
   * Many other helpful methods are available in the model.
   *
   */
  // ------------------------------------------------------------------------------------------------

  var move: Cell = null // this should hold the move that will be returned
  var pie = false // this should hold the pie rule decision
  var stop = false // used to end computation at completion of turn
  val lag = 50 // used for self-imposed time limit


  def makeMove(): Cell = {
    stop = false
    // Execute your move method with the given time restriction
    try { move = timedRun[Cell](timelimit - lag)(myMove()) }
    catch { case ex: Exception => } // something has gone wrong, such as a timeout
    stop = true // stop the computation within the method
    println(move)
    if (!model.legal(move)) move = randomMove(model)
    return move
  }

  def pieRule(firstmove: Cell): Boolean = {
    stop = false
    // Execute your pie method with the given time restriction
    try { pie = timedRun[Boolean](timelimit - lag)(myPie(firstmove)) }
    catch { case ex: Exception => } // something has gone wrong, such as a timeout
    stop = true // stop the computation within the method
    return pie
  }
  private def randomMove(mod: Model): Cell = {
    val open = mod.myCells(O)
    val randmove = open((Math.random() * open.length).toInt)
    println("Move chosen randomly: " + randmove.toString())
    randmove
  }
}
