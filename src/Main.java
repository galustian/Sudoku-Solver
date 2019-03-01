import java.util.*;

public class Main {
    public static void main(String[] args) {
        var sudokuBoard = getSudokuBoardFromFile("sudoku.txt");
        var solvedBoard = getSolvedBoard(sudokuBoard);
    }

    private static Board getSolvedBoard(Board board) {
        var pos = board.getPosWithLeastPossibilities();
        var possibilities = board.possibilitiesAt(pos.x, pos.y);

        for (int num : possibilities) {
            var newBoard = new Board(board);

            var validBoard = newBoard.setXYToNum(pos.x, pos.y, num);
            if (!validBoard) continue;

            assert newBoard.isValidBoard();

            if (newBoard.numEmptyPositions() == 0)
                return newBoard;

            var solvedBoard = getSolvedBoard(newBoard);
            if (solvedBoard != null)
                return solvedBoard;
        }

        return null;
    }

    private static Board getSudokuBoardFromFile(String fileName) {

    }
}

class Board {
    private int[][] board;
    private List<List<Set<Integer>>> boardPossibilities;

    Board(int[][] board) {
        this.board = board;
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (board[i][j] != 0) continue;
                fillPossiblePositions(i, j);
            }
        }
    }

    Board(Board board) {
        this.board = new int[9][9];
        this.boardPossibilities = new ArrayList<>();

        for (int i = 0; i < 9; i++) {
            var setArray = new ArrayList<Set<Integer>>();
            for (int j = 0; j < 9; j++) {
                this.board[i][j] = board.numAt(i, j);
                setArray.add(new HashSet<>(board.boardPossibilities.get(i).get(j)));
            }
            this.boardPossibilities.add(setArray);
        }
    }

    int numAt(int x, int y) {
        return board[x][y];
    }

    Set<Integer> possibilitiesAt(int x, int y) {
        return boardPossibilities.get(x).get(y);
    }

    // board is assumed to be empty
    void fillPossiblePositions(int x, int y) {
        var possibleNums = new HashSet<Integer>();
        for (int i = 1; i <= 9; i++) possibleNums.add(i);

        // remove nums from 3x3 square
        var startX = (x / 3) * 3;
        var startY = (y / 3) * 3;
        for (int i = startX; i <= startX + 3; i++) {
            for (int j = startY; j <= startY + 3; j++) {
                possibleNums.remove(board[i][j]);
            }
        }
        // remove nums vertical and horizontal
        for (int i = 0; i < 9; i++) {
            possibleNums.remove(board[i][y]);
            possibleNums.remove(board[x][i]);
        }

        boardPossibilities.get(x).set(y, possibleNums);
        var validBoard = updateBoardPossibilities(x, y);
        assert validBoard;
    }

    Position getPosWithLeastPossibilities() {
        var leastNum = possibilitiesAt(0, 0).size();
        var leastPos = new Position(0, 0);
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (possibilitiesAt(i, j).size() == 0) continue;
                if (possibilitiesAt(i, j).size() < leastNum) {
                    leastNum = possibilitiesAt(i, j).size();
                    leastPos = new Position(i, j);
                }
            }
        }
        return leastPos;
    }

    boolean setXYToNum(int x, int y, int num) {
        board[x][y] = num;
        return updateBoardPossibilities(x, y);
    }

    int numEmptyPositions() {
        return -1;
    }

    private boolean updateBoardPossibilities(int x, int y) {
        possibilitiesAt(x, y).removeAll(possibilitiesAt(x, y));

        // remove possibilities square
        var startX = (x / 3) * 3;
        var startY = (y / 3) * 3;
        for (int i = startX; i < startX + 3; i++) {
            for (int j = startY; j < startY + 3; j++) {
                possibilitiesAt(i, j).remove(board[x][y]);

                if (possibilitiesAt(i, j).size() == 1)
                    setXYToNum(i, j, possibilitiesAt(i, j).iterator().next());
            }
        }
        // remove possibilities vertical and horizontal
        for (int i = 0; i < 9; i++) {
            possibilitiesAt(i, y).remove(board[x][y]);
            if (possibilitiesAt(i, y).size() == 1)
                setXYToNum(i, y, possibilitiesAt(i, y).iterator().next());

            possibilitiesAt(x, i).remove(board[x][y]);
            //assert possibilitiesAt(x, i).iterator().next().equals(possibilitiesAt(x, i).stream().findFirst().get());
            if (possibilitiesAt(x, i).size() == 1)
                setXYToNum(x, i, possibilitiesAt(x, i).iterator().next());
        }

        // set obvious fields (heuristic)
        var validBoard = true;
        for (int i = 0; i < 9; i++) {
            validBoard = setObviousPossibilities(i, y);
            if (!validBoard) return false;
            validBoard = setObviousPossibilities(x, i);
            if (!validBoard) return false;
        }
        for (int i = startX; i < startX + 3; i++) {
            for (int j = startY; j < startY + 3; j++) {
                validBoard = setObviousPossibilities(x, i);
                if (!validBoard) return false;
            }
        }

        return isValidBoard();
    }

    // if a field has a possibility that no other field in its row/col/square has =>
    // set that possibility as XY
    private boolean setObviousPossibilities(int x, int y) {
        for (var p : possibilitiesAt(x, y)) {
            // check in square START
            var startX = (x / 3) * 3;
            var startY = (y / 3) * 3;
            var possibleInSquare = true;
            for (int i = startX; i < startX + 3; i++) {
                for (int j = startY; j < startY + 3; j++) {
                    if (possibilitiesAt(i, j).contains(p)) {
                        possibleInSquare = false;
                        break;
                    }
                }
                if (!possibleInSquare) break;
            }

            if (possibleInSquare)
                return setXYToNum(x, y, p);
            // check in square END

            // check in vertical/horizontal START
            var possibleInHorizontal = true;
            var possibleInVertical = true;
            for (int i = 0; i < 9; i++) {
                if (possibilitiesAt(i, y).contains(p)) {
                    possibleInHorizontal = false;
                    break;
                }
            }
            for (int i = 0; i < 9; i++) {
                if (possibilitiesAt(x, i).contains(p)) {
                    possibleInVertical = false;
                    break;
                }
            }

            if (possibleInHorizontal)
                return setXYToNum(x, y, p);
            if (possibleInVertical)
                return setXYToNum(x, y, p);
            // check in vertical/horizontal END
        }
        return true;
    }

    boolean isValidBoard() {
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (board[i][j] == 0 && possibilitiesAt(i, j).size() == 0)
                    return false;
            }
        }
        return true;
    }
}

class Position {
    int x;
    int y;
    Position(int x, int y) {
        this.x = x;
        this.y = y;
    }
}

/*
possibilitiesAt(x, y).removeAll(possibilitiesAt(x, y));

var modifiedPositions = new LinkedList<Position>();
var modifiedPositionsSet = new HashSet<Position>();
modifiedPositions.add(new Position(x, y));
modifiedPositionsSet.add(new Position(x, y));

var startX = (x / 3) * 3;
var startY = (y / 3) * 3;
for (int i = startX; i <= startX + 3; i++) {
    for (int j = startY; j <= startY + 3; j++) {
        if (possibilitiesAt(i, j).contains(board[x][y])) {
            possibilitiesAt(i, j).remove(board[x][y]);

            boolean added = modifiedPositionsSet.add(new Position(i, j));
            if (added) modifiedPositions.add(new Position(i, j));

            if (possibilitiesAt(i, j).size() == 1)
                setXYToNum(i, j, possibilitiesAt(i, j).stream().findFirst().get());
        }
    }
}
// remove possibilities vertical and horizontal
for (int i = 0; i < 9; i++) {
    if (possibilitiesAt(i, y).contains(board[x][y])) {
        possibilitiesAt(i, y).remove(board[x][y]);

        boolean added = modifiedPositionsSet.add(new Position(i, y));
        if (added) modifiedPositions.add(new Position(i, y));

        if (possibilitiesAt(i, y).size() == 1)
            setXYToNum(i, y, possibilitiesAt(i, y).stream().findFirst().get());
    }
    if (possibilitiesAt(x, i).contains(board[x][y])) {
        possibilitiesAt(x, i).remove(board[x][y]);

        boolean added = modifiedPositionsSet.add(new Position(x, i));
        if (added) modifiedPositions.add(new Position(x, i));

        if (possibilitiesAt(x, i).size() == 1)
            setXYToNum(x, i, possibilitiesAt(x, i).stream().findFirst().get());
    }
}

// for every modified position
// check if possibilities in same square, row or column change as a result
// if so => change possibilities
for (var pos : modifiedPositions) {
    // remove possibilities square START
    startX = (pos.x / 3) * 3;
    startY = (pos.y / 3) * 3;

    var numSquareFieldsWithSamePossibilities = 0;
    for (int i = startX; i <= startX + 3; i++) {
        for (int j = startY; j <= startY + 3; j++) {
            if (possibilitiesAt(i, j).equals(possibilitiesAt(pos.x, pos.y))) {
                numSquareFieldsWithSamePossibilities++;
            }
        }
    }
    if (numSquareFieldsWithSamePossibilities == possibilitiesAt(pos.x, pos.y).size()) {
        for (int i = startX; i <= startX + 3; i++) {
            for (int j = startY; j <= startY + 3; j++) {

                if (possibilitiesAt(i, j).equals(possibilitiesAt(pos.x, pos.y)))
                    continue;

                boolean anyElementRemoved = possibilitiesAt(i, j).removeAll(possibilitiesAt(pos.x, pos.y));
                if (anyElementRemoved) {
                    boolean added = modifiedPositionsSet.add(new Position(i, j));
                    if (added) modifiedPositions.add(new Position(i, j));

                    if (possibilitiesAt(i, j).size() == 1)
                        setXYToNum(i, j, possibilitiesAt(i, j).stream().findFirst().get());
                }
            }
        }
    }
    // remove possibilities square END

    // remove possibilities vertical and horizontal START
    var numFieldsInRowWithSamePossibilities = 0;
    var numFieldsInColWithSamePossibilities = 0;
    for (int i = 0; i < 9; i++) {
        if (possibilitiesAt(i, y).equals(possibilitiesAt(pos.x, pos.y))) {
            numFieldsInRowWithSamePossibilities++;
        }
        if (possibilitiesAt(x, i).equals(possibilitiesAt(pos.x, pos.y))) {
            numFieldsInColWithSamePossibilities++;
        }
    }
    if (numFieldsInRowWithSamePossibilities == possibilitiesAt(pos.x, pos.y).size()) {
        for (int i = 0; i < 9; i++) {
            if (possibilitiesAt(i, y).equals(possibilitiesAt(pos.x, pos.y)))
                continue;

            boolean anyElementRemoved = possibilitiesAt(i, y).removeAll(possibilitiesAt(pos.x, pos.y));
            if (anyElementRemoved) {
                boolean added = modifiedPositionsSet.add(new Position(i, y));
                if (added) modifiedPositions.add(new Position(i, y));

                if (possibilitiesAt(i, y).size() == 1)
                    setXYToNum(i, y, possibilitiesAt(i, y).stream().findFirst().get());
            }
        }
    }
    if (numFieldsInColWithSamePossibilities == possibilitiesAt(pos.x, pos.y).size()) {
        for (int i = 0; i < 9; i++) {
            if (possibilitiesAt(x, i).equals(possibilitiesAt(pos.x, pos.y)))
                continue;

            boolean anyElementRemoved = possibilitiesAt(x, i).removeAll(possibilitiesAt(pos.x, pos.y));
            if (anyElementRemoved) {
                boolean added = modifiedPositionsSet.add(new Position(x, i));
                if (added) modifiedPositions.add(new Position(x, i));

                if (possibilitiesAt(x, i).size() == 1)
                    setXYToNum(x, i, possibilitiesAt(x, i).stream().findFirst().get());
            }
        }
    }
    // remove possibilities vertical and horizontal END
}
*/