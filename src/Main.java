import java.io.File;
import java.io.IOException;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        var sudokuBoards = getSudokuBoardsFromFile("sudoku.txt");
        for (var sudokuBoard : sudokuBoards) {
            System.out.println("\nUnsolved:");
            printSudokuBoard(sudokuBoard.board);

            var solvedBoard = getSolvedBoard(sudokuBoard);
            System.out.println("\nSolved:");
            printSudokuBoard(solvedBoard.board);
        }
    }

    private static Board getSolvedBoard(Board board) {
        var pos = board.getPosWithLeastPossibilities();
        var possibilities = board.possibilitiesAt(pos.x, pos.y);

        for (int num : possibilities) {
            var newBoard = new Board(board);

            var validBoard = newBoard.setXYToNum(pos.x, pos.y, num);
            if (!validBoard) continue;

            if (newBoard.numEmptyPositions() == 0)
                return newBoard;

            var solvedBoard = getSolvedBoard(newBoard);
            if (solvedBoard != null)
                return solvedBoard;
        }

        return null;
    }

    private static List<Board> getSudokuBoardsFromFile(String fileName) {
        var allBoards = new ArrayList<Board>();

        try (var scanner = new Scanner(new File(fileName))) {
            int[][] board = new int[9][9];
            var lineCount = 0;

            while (scanner.hasNext()) {
                var line = scanner.nextLine();
                if (line.contains("Grid")) continue;

                var charArray = line.toCharArray();
                for (int i = 0; i < 9; i++)
                    board[i][lineCount] = Character.getNumericValue(charArray[i]);

                lineCount++;
                if (lineCount == 9) {
                    allBoards.add(new Board(board));
                    board = new int[9][9];
                    lineCount = 0;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return allBoards;
    }

    private static void printSudokuBoard(int[][] board) {
        for (int i = 0; i < 9; i++) {
            var horizontalString = "";
            for (int j = 0; j < 9; j++) {
                if (j != 0 && j % 3 == 0) horizontalString += " | ";
                horizontalString += board[j][i];
                if ((j + 1) % 3 != 0) horizontalString += " ";
            }
            if (i != 0 && i % 3 == 0) System.out.println("- - - - - - - - - - -");
            System.out.println(horizontalString);
        }
    }
}

class Board {
    int[][] board;
    private List<List<Set<Integer>>> boardPossibilities;

    Board(int[][] board) {
        this.board = board;
        this.boardPossibilities = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            var hashSetList = new ArrayList<Set<Integer>>();
            for (int j = 0; j < 9; j++) {
                hashSetList.add(new HashSet<>());
            }
            this.boardPossibilities.add(hashSetList);
        }

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
                this.board[i][j] = board.board[i][j];
                setArray.add(new HashSet<>(board.possibilitiesAt(i, j)));
            }
            this.boardPossibilities.add(setArray);
        }
    }

    // board is assumed to be empty
    void fillPossiblePositions(int x, int y) {
        var possibleNums = new HashSet<Integer>();
        for (int i = 1; i <= 9; i++) possibleNums.add(i);

        // remove nums from 3x3 square
        var startX = (x / 3) * 3;
        var startY = (y / 3) * 3;
        for (int i = startX; i < startX + 3; i++) {
            for (int j = startY; j < startY + 3; j++) {
                possibleNums.remove(board[i][j]);
            }
        }
        // remove nums vertical and horizontal
        for (int i = 0; i < 9; i++) {
            possibleNums.remove(board[i][y]);
            possibleNums.remove(board[x][i]);
        }

        boardPossibilities.get(x).set(y, possibleNums);
    }

    Position getPosWithLeastPossibilities() {
        var leastNum = 10;
        Position leastPos = null;
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

    private boolean updateBoardPossibilities(int x, int y) {
        possibilitiesAt(x, y).removeAll(possibilitiesAt(x, y));

        // remove possibilities square
        var startX = (x / 3) * 3;
        var startY = (y / 3) * 3;
        for (int i = startX; i < startX + 3; i++) {
            for (int j = startY; j < startY + 3; j++)
                possibilitiesAt(i, j).remove(board[x][y]);
        }
        // remove possibilities vertical and horizontal
        for (int i = 0; i < 9; i++) {
            possibilitiesAt(i, y).remove(board[x][y]);
            possibilitiesAt(x, i).remove(board[x][y]);
        }

        // if 1 possibility, setXY
        for (int i = startX; i < startX + 3; i++) {
            for (int j = startY; j < startY + 3; j++) {
                if (possibilitiesAt(i, j).size() == 1)
                    setXYToNum(i, j, possibilitiesAt(i, j).iterator().next());
            }
        }
        for (int i = 0; i < 9; i++) {
            if (possibilitiesAt(i, y).size() == 1)
                setXYToNum(i, y, possibilitiesAt(i, y).iterator().next());
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
        var startX = (x / 3) * 3;
        var startY = (y / 3) * 3;

        for (int p : possibilitiesAt(x, y)) {
            // check in square START
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

            if (possibleInSquare) return setXYToNum(x, y, p);
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

            if (possibleInHorizontal) return setXYToNum(x, y, p);
            if (possibleInVertical) return setXYToNum(x, y, p);
            // check in vertical/horizontal END
        }
        return true;
    }

    boolean isValidBoard() {
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (board[i][j] == 0 && possibilitiesAt(i, j).size() == 0) return false;
            }
        }
        return true;
    }

    int numEmptyPositions() {
        int emptyCount = 0;
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++){
                if (board[i][j] == 0) emptyCount++;
            }
        }
        return emptyCount;
    }

    Set<Integer> possibilitiesAt(int x, int y) {
        return boardPossibilities.get(x).get(y);
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
